package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.network.GitHubApiService
import javax.inject.Inject

interface UserRepository {
    suspend fun getCurrentUser(): User
}

class UserRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubApiService,
    ) : UserRepository {
        override suspend fun getCurrentUser(): User {
            val dto = api.getAuthenticatedUser()
            return User(
                login = dto.login,
                name = dto.name,
                avatarUrl = dto.avatarUrl,
                publicRepos = dto.publicRepos,
                followers = dto.followers,
                following = dto.following,
            )
        }
    }
