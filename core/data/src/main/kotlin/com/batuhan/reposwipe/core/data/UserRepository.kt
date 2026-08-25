package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.network.GitHubApiService
import javax.inject.Inject

interface UserRepository {
    suspend fun getCurrentUser(): User

    /** The authenticated user's own repos, most recently updated first. */
    suspend fun getRecentRepos(limit: Int): List<Repo>

    /** Every login the authenticated user follows, across all pages — used to cross-reference
     * "am I already following this person" against a followers list without an N+1 API call. */
    suspend fun getAllFollowingLogins(): Set<String>
}

class UserRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubApiService,
    ) : UserRepository {
        override suspend fun getCurrentUser(): User = api.getAuthenticatedUser().toDomain()

        override suspend fun getRecentRepos(limit: Int): List<Repo> = api.getUserRepos(perPage = limit).map { it.toDomain() }

        override suspend fun getAllFollowingLogins(): Set<String> {
            val logins = mutableSetOf<String>()
            var page = 1
            var batchSize: Int
            do {
                val batch = api.getFollowing(page = page)
                logins += batch.map { it.login }
                batchSize = batch.size
                page++
            } while (batchSize >= FOLLOWING_PAGE_SIZE)
            return logins
        }

        private companion object {
            const val FOLLOWING_PAGE_SIZE = 30
        }
    }
