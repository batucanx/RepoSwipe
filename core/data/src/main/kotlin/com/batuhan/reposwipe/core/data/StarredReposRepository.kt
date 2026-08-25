package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.network.GitHubApiService
import javax.inject.Inject

interface StarredReposRepository {
    /** One page of the authenticated user's starred repos, newest-starred first. */
    suspend fun getStarredReposPage(
        page: Int,
        perPage: Int = 30,
    ): List<Repo>
}

class StarredReposRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubApiService,
    ) : StarredReposRepository {
        override suspend fun getStarredReposPage(
            page: Int,
            perPage: Int,
        ): List<Repo> = api.getStarredRepos(page = page, perPage = perPage).map { it.toDomain() }
    }
