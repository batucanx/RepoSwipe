package com.batuhan.reposwipe.core.data

import androidx.annotation.VisibleForTesting
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
import kotlin.math.ceil
import kotlin.random.Random

/**
 * GitHub Search API caps any single query at 1000 results (`page * per_page <= 1000`) — once
 * hit, pagination simply ends rather than erroring, same as running out of real results.
 *
 * Randomization: without this, every cold start/filter change would deterministically fetch page
 * 1 sorted by stars descending — always the same handful of mega-repos first. A single mediator
 * instance lives for one [androidx.paging.Pager] subscription (one cold start / one filter
 * change), so picking a random sort + start page in [LoadType.REFRESH] and reusing it for
 * subsequent [LoadType.APPEND] calls gives fresh variety per session while keeping pagination
 * within that session gap/duplicate-free. Each fetched page is shuffled client-side before being
 * written to Room via [RepoEntity.fetchOrder], since GitHub's own within-page ordering is itself
 * sorted and [com.batuhan.reposwipe.core.database.RepoDao.pagingSource] no longer sorts by star
 * count.
 */
@OptIn(ExperimentalPagingApi::class)
class RepoRemoteMediator(
    private val query: String,
    private val api: GitHubApiService,
    private val database: AppDatabase,
) : RemoteMediator<Int, RepoEntity>() {
    private val repoDao = database.repoDao()
    private val remoteKeyDao = database.remoteKeyDao()

    private var activeSort: String = RANDOM_SORT_OPTIONS.first()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RepoEntity>,
    ): MediatorResult {
        val page =
            when (loadType) {
                LoadType.REFRESH -> {
                    activeSort = RANDOM_SORT_OPTIONS.random()
                    Random.nextInt(1, RANDOM_START_PAGE_POOL + 1)
                }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem =
                        state.lastItemOrNull()
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                    val remoteKey =
                        remoteKeyDao.remoteKeyByRepoId(lastItem.id, query)
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                    remoteKey.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

        return try {
            val pageSize = state.config.pageSize
            var response =
                api.searchRepositories(query = query, page = page, perPage = pageSize, sort = activeSort, order = ORDER)
            var effectivePage = page

            if (loadType == LoadType.REFRESH && response.items.isEmpty() && response.totalCount > 0) {
                // The random start page landed beyond this query's actual result set (a narrow
                // filter, or this sort dimension simply has fewer matches) — retry once with a
                // page that's guaranteed valid for the now-known total. Bounded to 2 network
                // calls total; a genuinely zero-result query still resolves to an empty page.
                val range = validPageRange(response.totalCount, pageSize, RANDOM_START_PAGE_POOL)
                effectivePage = Random.nextInt(range.first, range.last + 1)
                response =
                    api.searchRepositories(
                        query = query,
                        page = effectivePage,
                        perPage = pageSize,
                        sort = activeSort,
                        order = ORDER,
                    )
            }

            val repos = response.items
            val endOfPaginationReached = isEndOfPagination(repos.isEmpty(), effectivePage, pageSize, MAX_SEARCH_RESULTS)
            val nextPage = if (endOfPaginationReached) null else effectivePage + 1

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    repoDao.clearForQuery(query)
                    remoteKeyDao.clearForQuery(query)
                }
                remoteKeyDao.insertAll(
                    repos.map { RemoteKeyEntity(repoId = it.id, query = query, nextPage = nextPage) },
                )
                val fetchOrderBase = effectivePage * pageSize
                repoDao.insertAll(
                    repos.shuffled().mapIndexed { index, dto ->
                        dto.toEntity(query = query, fetchOrder = fetchOrderBase + index)
                    },
                )
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    @VisibleForTesting
    internal companion object {
        const val MAX_SEARCH_RESULTS = 1000
        const val ORDER = "desc"
        const val RANDOM_START_PAGE_POOL = 10
        val RANDOM_SORT_OPTIONS = listOf("stars", "forks", "updated")

        /**
         * The inclusive page range that's safe to land a REFRESH retry on, given how many total
         * results the query actually has — never wider than [pagePool], and always at least `1..1`
         * so a narrow filter with fewer than [pagePool] pages still resolves to a valid page.
         */
        fun validPageRange(
            totalCount: Int,
            pageSize: Int,
            pagePool: Int,
        ): IntRange {
            val maxValidPage = maxOf(1, ceil(totalCount.toDouble() / pageSize).toInt())
            return 1..minOf(maxValidPage, pagePool)
        }

        fun isEndOfPagination(
            itemsEmpty: Boolean,
            page: Int,
            pageSize: Int,
            maxResults: Int,
        ): Boolean = itemsEmpty || page * pageSize >= maxResults
    }
}
