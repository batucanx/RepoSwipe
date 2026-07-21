package com.batuhan.reposwipe.core.data.di

import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.data.DiscoverFilterRepositoryImpl
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import com.batuhan.reposwipe.core.data.LeaderboardRepositoryImpl
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.RepoRepositoryImpl
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.StarRepositoryImpl
import com.batuhan.reposwipe.core.data.StarredReposRepository
import com.batuhan.reposwipe.core.data.StarredReposRepositoryImpl
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.UserRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindRepoRepository(impl: RepoRepositoryImpl): RepoRepository

    @Binds
    abstract fun bindStarRepository(impl: StarRepositoryImpl): StarRepository

    @Binds
    abstract fun bindStarredReposRepository(impl: StarredReposRepositoryImpl): StarredReposRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindLeaderboardRepository(impl: LeaderboardRepositoryImpl): LeaderboardRepository

    @Singleton
    @Binds
    abstract fun bindDiscoverFilterRepository(impl: DiscoverFilterRepositoryImpl): DiscoverFilterRepository

    companion object {
        @Provides
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    }
}
