package com.batuhan.reposwipe.feature.auth.di

import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.batuhan.reposwipe.feature.auth.data.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
