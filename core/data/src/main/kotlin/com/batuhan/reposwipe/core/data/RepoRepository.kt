package com.batuhan.reposwipe.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.network.GitHubApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface RepoRepository {
    fun searchRepos(language: String?): Flow<PagingData<Repo>>
}

class RepoRepositoryImpl @Inject constructor(
    private val api: GitHubApiService,
    private val database: AppDatabase,
) : RepoRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun searchRepos(language: String?): Flow<PagingData<Repo>> {
        val query = buildQuery(language)
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            remoteMediator = RepoRemoteMediator(query = query, api = api, database = database),
            pagingSourceFactory = { database.repoDao().pagingSource() },
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    /**
     * No official GitHub "trending" endpoint exists, so this approximates it: repos created in
     * the last 6 months with a meaningful star count, sorted by stars. `feature:leaderboard`'s
     * "Trending Today" is a different, unrelated thing — our own Firestore swipe-activity
     * aggregate, added in a later phase.
     */
    private fun buildQuery(language: String?): String {
        val sinceDate = LocalDate.now().minusMonths(6)
        val base = "created:>$sinceDate stars:>50"
        return if (language.isNullOrBlank()) base else "$base language:$language"
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
