package com.batuhan.reposwipe.core.database.di

import android.content.Context
import androidx.room.Room
import com.batuhan.reposwipe.core.database.AppDatabase
import com.batuhan.reposwipe.core.database.RemoteKeyDao
import com.batuhan.reposwipe.core.database.RepoDao
import com.batuhan.reposwipe.core.database.StarOutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "reposwipe.db")
            // No shipped users/data yet — revisit with real Migrations before a public release.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRepoDao(database: AppDatabase): RepoDao = database.repoDao()

    @Provides
    fun provideRemoteKeyDao(database: AppDatabase): RemoteKeyDao = database.remoteKeyDao()

    @Provides
    fun provideStarOutboxDao(database: AppDatabase): StarOutboxDao = database.starOutboxDao()
}
