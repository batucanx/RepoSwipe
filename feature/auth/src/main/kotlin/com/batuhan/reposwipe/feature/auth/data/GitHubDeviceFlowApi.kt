package com.batuhan.reposwipe.feature.auth.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/** https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow */
interface GitHubDeviceFlowApi {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        // public_repo: star/unstar public repos. user:follow: follow/unfollow users — added for
        // the Followers/Following feature; a token issued before this change won't have it, the
        // user must sign out and sign back in to re-authorize with the new scope.
        @Field("scope") scope: String = "public_repo user:follow",
    ): DeviceCodeResponse

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun requestAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    ): AccessTokenResponse
}
