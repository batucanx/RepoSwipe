package com.batuhan.reposwipe.core.network

import com.batuhan.reposwipe.core.network.model.SearchRepositoriesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubApiService {

    /** https://docs.github.com/en/rest/search/search#search-repositories */
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
    ): SearchRepositoriesResponseDto
}
