package com.batuhan.reposwipe.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.batuhan.reposwipe.core.common.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesDataStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val themeMode: Flow<ThemeMode> =
            dataStore.data.map { preferences ->
                preferences[Keys.THEME_MODE]?.let { stored ->
                    runCatching { ThemeMode.valueOf(stored) }.getOrNull()
                } ?: ThemeMode.SYSTEM
            }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = mode.name }
        }

        private object Keys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
        }
    }
