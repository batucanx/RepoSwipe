package com.batuhan.reposwipe.core.network

import com.batuhan.reposwipe.core.network.model.RepoDto
import com.batuhan.reposwipe.core.network.model.SearchRepositoriesResponseDto
import com.batuhan.reposwipe.core.network.model.UserDto
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
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

    /** https://docs.github.com/en/rest/activity/starring#star-a-repository-for-the-authenticated-user */
    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: RequestBody = EmptyRequestBody,
    )

    /** https://docs.github.com/en/rest/activity/starring#unstar-a-repository-for-the-authenticated-user */
    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    )

    /** https://docs.github.com/en/rest/activity/starring#list-repositories-starred-by-the-authenticated-user */
    @GET("user/starred")
    suspend fun getStarredRepos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
    ): List<RepoDto>

    /** https://docs.github.com/en/rest/users/users#get-the-authenticated-user */
    @GET("user")
    suspend fun getAuthenticatedUser(): UserDto

    companion object {
        // GitHub requires an explicit (zero-length) body on this PUT — no @Body param at all
        // would omit Content-Length rather than sending it as 0.
        val EmptyRequestBody: RequestBody = "".toRequestBody(null)
    }
}
