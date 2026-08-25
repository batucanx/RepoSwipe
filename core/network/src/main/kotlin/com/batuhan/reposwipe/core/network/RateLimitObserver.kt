package com.batuhan.reposwipe.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RateLimitInfo(
    val remaining: Int,
    val resetEpochSeconds: Long,
) {
    val isExhausted: Boolean
        get() = remaining <= 0 && System.currentTimeMillis() / 1000 < resetEpochSeconds
}

/**
 * Holds the most recently observed `x-ratelimit-*` values (updated by [RateLimitInterceptor])
 * so the UI can show a cooldown state instead of letting requests fail silently.
 *
 * GitHub's Search API and core REST API are separate rate-limit buckets but share the same
 * header names — this only tracks whichever response was seen most recently, which is an
 * accepted simplification for a portfolio-scale app.
 */
@Singleton
class RateLimitObserver
    @Inject
    constructor() {
        private val _state = MutableStateFlow<RateLimitInfo?>(null)
        val state: StateFlow<RateLimitInfo?> = _state.asStateFlow()

        internal fun update(
            remaining: Int,
            resetEpochSeconds: Long,
        ) {
            _state.value = RateLimitInfo(remaining, resetEpochSeconds)
        }
    }
