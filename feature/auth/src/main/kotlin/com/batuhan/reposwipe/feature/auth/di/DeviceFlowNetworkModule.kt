package com.batuhan.reposwipe.feature.auth.di

import com.batuhan.reposwipe.feature.auth.data.GitHubDeviceFlowApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The device-flow endpoints live on `github.com`, not `api.github.com`, and are called before
 * any access token exists — so they get their own plain Retrofit instance rather than reusing
 * `core:network`'s auth-header-injecting client.
 *
 * Pinned to TLS 1.2: some Android emulator system images hit a BoringSSL TLS 1.3 handshake bug
 * against `github.com`'s edge specifically (`SSLHandshakeException ... DECODE_ERROR` in
 * `tls13_client.cc`) — `api.github.com` isn't affected since it terminates TLS differently. Real
 * devices aren't known to hit this; TLS 1.2 is still fully secure, just a compatibility fallback.
 */
@Module
@InstallIn(SingletonComponent::class)
object DeviceFlowNetworkModule {
    private val Tls12ConnectionSpec =
        ConnectionSpec
            .Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideGitHubDeviceFlowApi(json: Json): GitHubDeviceFlowApi =
        Retrofit
            .Builder()
            .baseUrl("https://github.com/")
            .client(
                OkHttpClient
                    .Builder()
                    .connectionSpecs(listOf(Tls12ConnectionSpec))
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build(),
            ).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubDeviceFlowApi::class.java)
}
