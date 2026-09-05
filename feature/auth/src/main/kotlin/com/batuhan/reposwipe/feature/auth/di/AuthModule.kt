package com.batuhan.reposwipe.feature.auth.di

import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.batuhan.reposwipe.feature.auth.data.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
