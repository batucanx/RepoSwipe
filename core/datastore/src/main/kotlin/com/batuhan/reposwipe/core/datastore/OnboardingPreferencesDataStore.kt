package com.batuhan.reposwipe.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferencesDataStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val hasCompletedOnboarding: Flow<Boolean> =
            dataStore.data.map { preferences -> preferences[Keys.ONBOARDING_COMPLETED] ?: false }

        suspend fun setOnboardingCompleted() {
            dataStore.edit { preferences -> preferences[Keys.ONBOARDING_COMPLETED] = true }
        }

        private object Keys {
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        }
    }
