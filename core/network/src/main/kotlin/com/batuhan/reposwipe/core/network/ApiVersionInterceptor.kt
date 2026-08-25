package com.batuhan.reposwipe.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Every GitHub REST API request must declare an API version and accept the v3+json media type —
 * except when a call already asked for a different one via a method-level Retrofit `@Headers`
 * (e.g. [GitHubApiService.getReadme] requesting `application/vnd.github.html+json`): `.header()`
 * *replaces* any existing value for that name, so applying the default unconditionally would
 * silently clobber that per-call override on every request, not just the readme one.
 */
class ApiVersionInterceptor
    @Inject
    constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val requestBuilder = original.newBuilder().header("X-GitHub-Api-Version", GitHubApiConstants.API_VERSION)
            if (original.header("Accept") == null) {
                requestBuilder.header("Accept", "application/vnd.github+json")
            }
            return chain.proceed(requestBuilder.build())
        }
    }
