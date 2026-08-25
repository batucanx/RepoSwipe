package com.batuhan.reposwipe.core.network.di

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
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

    /**
     * GitHub's REST API sends `ETag` on essentially every GET and `Cache-Control: private,
     * max-age=60` on authenticated ones, so an HTTP cache does real work here without any
     * hand-rolled staleness logic: re-opening a repo's detail sheet (README + languages +
     * contributors + similar repos — four separate GETs) serves from disk or revalidates with a
     * 304 instead of re-downloading. A 304 also doesn't count against the API rate limit, which
     * matters for a swipe-heavy app on GitHub's 5000/hour authenticated budget.
     *
     * Safe across accounts because GitHub sends `Vary: Accept, Authorization, Cookie,
     * X-Requested-With` — OkHttp keys cache entries by those headers, so a response fetched with
     * one user's token is never served to another's. [AuthInterceptor] adds that header, so it
     * runs as an application interceptor (before the cache), which is already the case here.
     */
    @Provides
    @Singleton
    fun provideHttpCache(
        @ApplicationContext context: Context,
    ): Cache = Cache(directory = File(context.cacheDir, HTTP_CACHE_DIR), maxSize = HTTP_CACHE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        apiVersionInterceptor: ApiVersionInterceptor,
        authInterceptor: AuthInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .cache(cache)
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
    private const val HTTP_CACHE_DIR = "github_http_cache"

    // Generous enough to hold a browsing session's worth of READMEs (the largest bodies by far —
    // GitHub's rendered HTML, not markdown) alongside the small JSON responses, while staying well
    // inside what's polite to occupy in an app's evictable cache dir.
    private const val HTTP_CACHE_BYTES = 25L * 1024 * 1024
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
