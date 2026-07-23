package com.batuhan.reposwipe.core.network.di

import com.batuhan.reposwipe.core.network.ApiVersionInterceptor
import com.batuhan.reposwipe.core.network.AuthInterceptor
import com.batuhan.reposwipe.core.network.BuildConfig
import com.batuhan.reposwipe.core.network.ConnectivityNetworkMonitor
import com.batuhan.reposwipe.core.network.GitHubApiConstants
import com.batuhan.reposwipe.core.network.GitHubApiService
import com.batuhan.reposwipe.core.network.NetworkMonitor
import com.batuhan.reposwipe.core.network.RateLimitInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@OptIn(ExperimentalSerializationApi::class)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** Shared across all GitHub JSON parsing (device flow + REST API), which is snake_case. */
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiVersionInterceptor: ApiVersionInterceptor,
        authInterceptor: AuthInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(apiVersionInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .apply {
                // Never in release: even BASIC level writes request URLs/response codes to Logcat.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideGitHubRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(GitHubApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService = retrofit.create(GitHubApiService::class.java)

    private const val TIMEOUT_SECONDS = 30L
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
