package com.batuhan.reposwipe.feature.auth.di

import com.batuhan.reposwipe.feature.auth.data.GitHubDeviceFlowApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * The device-flow endpoints live on `github.com`, not `api.github.com`, and are called before
 * any access token exists — so they get their own plain Retrofit instance rather than reusing
 * `core:network`'s auth-header-injecting client.
 */
@Module
@InstallIn(SingletonComponent::class)
object DeviceFlowNetworkModule {
    @Provides
    @Singleton
    fun provideGitHubDeviceFlowApi(json: Json): GitHubDeviceFlowApi =
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubDeviceFlowApi::class.java)
}
