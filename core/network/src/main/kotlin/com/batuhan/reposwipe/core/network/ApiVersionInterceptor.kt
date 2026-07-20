package com.batuhan.reposwipe.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Every GitHub REST API request must declare an API version and accept the v3+json media type. */
class ApiVersionInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", GitHubApiConstants.API_VERSION)
            .build()
        return chain.proceed(request)
    }
}
