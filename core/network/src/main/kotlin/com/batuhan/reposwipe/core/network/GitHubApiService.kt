package com.batuhan.reposwipe.core.network

import com.batuhan.reposwipe.core.network.model.ContributorDto
import com.batuhan.reposwipe.core.network.model.RepoDto
import com.batuhan.reposwipe.core.network.model.SearchRepositoriesResponseDto
import com.batuhan.reposwipe.core.network.model.UserDto
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
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

    /**
     * https://docs.github.com/en/rest/activity/starring#check-if-a-repository-is-starred-by-the-authenticated-user
     *
     * Returns `Response<Unit>` rather than Unit because GitHub answers with 204 (starred) or
     * 404 (not starred) — a bare suspend fun would surface the 404 as a thrown HttpException.
     */
    @GET("user/starred/{owner}/{repo}")
    suspend fun isRepoStarred(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<Unit>

    /** https://docs.github.com/en/rest/activity/starring#list-repositories-starred-by-the-authenticated-user */
    @GET("user/starred")
    suspend fun getStarredRepos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
    ): List<RepoDto>

    /** https://docs.github.com/en/rest/users/users#get-the-authenticated-user */
    @GET("user")
    suspend fun getAuthenticatedUser(): UserDto

    /** https://docs.github.com/en/rest/repos/repos#list-repositories-for-the-authenticated-user */
    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 5,
    ): List<RepoDto>

    /**
     * https://docs.github.com/en/rest/repos/contents#get-a-repository-readme
     *
     * The `html+json` media type makes GitHub return the README as its own already-rendered
     * HTML (the exact markup github.com's repo page shows) rather than a JSON envelope wrapping
     * base64 markdown — the response body is raw HTML text, so this bypasses the kotlinx
     * serialization converter entirely by returning [ResponseBody] directly.
     */
    @Headers("Accept: application/vnd.github.html+json")
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): ResponseBody

    /** https://docs.github.com/en/rest/repos/repos#list-repository-languages — byte count per language. */
    @GET("repos/{owner}/{repo}/languages")
    suspend fun getLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Map<String, Long>

    /** https://docs.github.com/en/rest/repos/repos#list-repository-contributors */
    @GET("repos/{owner}/{repo}/contributors")
    suspend fun getContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10,
    ): List<ContributorDto>

    /** https://docs.github.com/en/rest/users/followers#list-followers-of-the-authenticated-user */
    @GET("user/followers")
    suspend fun getFollowers(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
    ): List<UserDto>

    /** https://docs.github.com/en/rest/users/followers#list-the-people-the-authenticated-user-follows */
    @GET("user/following")
    suspend fun getFollowing(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
    ): List<UserDto>

    /** https://docs.github.com/en/rest/users/followers#follow-a-user */
    @PUT("user/following/{username}")
    suspend fun followUser(
        @Path("username") username: String,
        @Body body: RequestBody = EmptyRequestBody,
    )

    /** https://docs.github.com/en/rest/users/followers#unfollow-a-user */
    @DELETE("user/following/{username}")
    suspend fun unfollowUser(
        @Path("username") username: String,
    )

    companion object {
        // GitHub requires an explicit (zero-length) body on this PUT — no @Body param at all
        // would omit Content-Length rather than sending it as 0.
        val EmptyRequestBody: RequestBody = "".toRequestBody(null)
    }
}
