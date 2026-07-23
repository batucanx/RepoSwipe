package com.batuhan.reposwipe.core.common.auth

/**
 * Read-side access to the stored GitHub access token, kept in `core:common` so
 * `core:network` (AuthInterceptor) and `core:datastore` (the concrete implementation) don't
 * need to depend on each other directly.
 */
interface TokenProvider {
    suspend fun getToken(): String?

    suspend fun clearToken()
}
