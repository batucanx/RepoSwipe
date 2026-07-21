package com.batuhan.reposwipe.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RateLimitInterceptor
    @Inject
    constructor(
        private val observer: RateLimitObserver,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val remaining = response.header("x-ratelimit-remaining")?.toIntOrNull()
            val reset = response.header("x-ratelimit-reset")?.toLongOrNull()
            if (remaining != null && reset != null) {
                observer.update(remaining, reset)
            }
            return response
        }
    }
