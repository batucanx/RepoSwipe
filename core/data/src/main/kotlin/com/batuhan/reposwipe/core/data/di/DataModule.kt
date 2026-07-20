package com.batuhan.reposwipe.core.data.di

import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.RepoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindRepoRepository(impl: RepoRepositoryImpl): RepoRepository
}
