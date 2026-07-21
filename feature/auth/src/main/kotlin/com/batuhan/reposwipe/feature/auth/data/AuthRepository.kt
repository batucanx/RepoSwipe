package com.batuhan.reposwipe.feature.auth.data

import com.batuhan.reposwipe.core.datastore.TokenDataStore
import com.batuhan.reposwipe.feature.auth.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    suspend fun requestDeviceCode(): DeviceCodeResponse

    suspend fun pollAccessToken(deviceCode: String): AccessTokenResponse

    suspend fun saveToken(token: String)

    suspend fun signOut()
}

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: GitHubDeviceFlowApi,
        private val tokenDataStore: TokenDataStore,
    ) : AuthRepository {
        override val isAuthenticated: Flow<Boolean> =
            tokenDataStore.accessToken.map { !it.isNullOrBlank() }

        override suspend fun requestDeviceCode(): DeviceCodeResponse =
            api.requestDeviceCode(
                clientId = BuildConfig.GITHUB_CLIENT_ID,
            )

        override suspend fun pollAccessToken(deviceCode: String): AccessTokenResponse =
            api.requestAccessToken(clientId = BuildConfig.GITHUB_CLIENT_ID, deviceCode = deviceCode)

        override suspend fun saveToken(token: String) = tokenDataStore.saveAccessToken(token)

        override suspend fun signOut() = tokenDataStore.clearAccessToken()
    }
