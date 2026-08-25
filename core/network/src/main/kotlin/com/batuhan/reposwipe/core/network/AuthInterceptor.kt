package com.batuhan.reposwipe.core.network

import com.batuhan.reposwipe.core.common.auth.TokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches the stored GitHub access token, when present. OkHttp interceptors run on a
 * background dispatcher already, so blocking on the (cheap, in-memory-cached-after-first-read)
 * [TokenProvider] suspend call here is safe and is the standard pattern for auth interceptors.
 *
 * A 401 means GitHub has rejected the token (revoked by the user, expired, etc.) — it's cleared
 * immediately so the app doesn't keep retrying with dead credentials, and so anything observing
 * token/auth state (e.g. the app's nav host) notices and routes back to sign-in.
 */
class AuthInterceptor
    @Inject
    constructor(
        private val tokenProvider: TokenProvider,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = runBlocking { tokenProvider.getToken() }
            val request =
                if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain
                        .request()
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
            val response = chain.proceed(request)
            if (response.code == 401 && !token.isNullOrBlank()) {
                runBlocking { tokenProvider.clearToken() }
            }
            return response
        }
    }
