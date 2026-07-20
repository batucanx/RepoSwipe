package com.batuhan.reposwipe.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.batuhan.reposwipe.core.common.auth.TokenProvider
import com.batuhan.reposwipe.core.datastore.TokenDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideAuthPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreBindsModule {
    @Binds
    abstract fun bindTokenProvider(tokenDataStore: TokenDataStore): TokenProvider
}
