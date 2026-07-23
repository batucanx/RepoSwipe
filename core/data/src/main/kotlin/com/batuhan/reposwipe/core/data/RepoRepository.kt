package com.batuhan.reposwipe.core.data

import androidx.annotation.VisibleForTesting
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.network.GitHubApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface RepoRepository {
    fun searchRepos(filters: DiscoverFilters): Flow<PagingData<Repo>>
}

class RepoRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubApiService,
        private val database: AppDatabase,
    ) : RepoRepository {
        @OptIn(ExperimentalPagingApi::class)
        override fun searchRepos(filters: DiscoverFilters): Flow<PagingData<Repo>> {
            val query = buildQuery(filters)
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
        @VisibleForTesting
        internal fun buildQuery(filters: DiscoverFilters): String {
            val sinceDate = LocalDate.now().minusMonths(6)
            val minStars = maxOf(MIN_STARS_FLOOR, filters.minStars)
            val parts = mutableListOf("created:>$sinceDate", "stars:>=$minStars")

            if (filters.languages.isNotEmpty()) {
                parts += filters.languages.joinToString(separator = " OR ", prefix = "(", postfix = ")") { "language:$it" }
            }
            if (filters.topics.isNotEmpty()) {
                parts += filters.topics.joinToString(separator = " OR ", prefix = "(", postfix = ")") { "topic:$it" }
            }
            if (filters.updatedRecently) {
                parts += "pushed:>${LocalDate.now().minusDays(RECENT_DAYS)}"
            }

            return parts.joinToString(" ")
        }

        @VisibleForTesting
        internal companion object {
            const val PAGE_SIZE = 20
            const val MIN_STARS_FLOOR = 50
            const val RECENT_DAYS = 7L
        }
    }
