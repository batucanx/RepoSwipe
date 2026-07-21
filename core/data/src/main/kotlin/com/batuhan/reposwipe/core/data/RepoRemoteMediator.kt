package com.batuhan.reposwipe.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.batuhan.reposwipe.core.data.mapper.toEntity
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.database.RemoteKeyEntity
import com.batuhan.reposwipe.core.database.RepoEntity
import com.batuhan.reposwipe.core.network.GitHubApiService
import retrofit2.HttpException
import java.io.IOException

/**
 * GitHub Search API caps any single query at 1000 results (`page * per_page <= 1000`) — once
 * hit, pagination simply ends rather than erroring, same as running out of real results.
 */
@OptIn(ExperimentalPagingApi::class)
class RepoRemoteMediator(
    private val query: String,
    private val api: GitHubApiService,
    private val database: AppDatabase,
) : RemoteMediator<Int, RepoEntity>() {
    private val repoDao = database.repoDao()
    private val remoteKeyDao = database.remoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RepoEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem =
                        state.lastItemOrNull()
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val remoteKey =
                        remoteKeyDao.remoteKeyByRepoId(lastItem.id)
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                    remoteKey.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

        return try {
            val pageSize = state.config.pageSize
            val response = api.searchRepositories(query = query, page = page, perPage = pageSize)
            val repos = response.items
            val endOfPaginationReached = repos.isEmpty() || page * pageSize >= MAX_SEARCH_RESULTS
            val nextPage = if (endOfPaginationReached) null else page + 1

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    repoDao.clearAll()
                    remoteKeyDao.clearAll()
                }
                remoteKeyDao.insertAll(repos.map { RemoteKeyEntity(repoId = it.id, nextPage = nextPage) })
                repoDao.insertAll(repos.map { it.toEntity() })
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 1000
    }
}
