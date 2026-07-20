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
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider.getToken() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
