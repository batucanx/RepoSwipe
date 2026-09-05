package com.batuhan.reposwipe.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.datastore.ThemePreferencesDataStore
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val themePreferencesDataStore: ThemePreferencesDataStore,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                themePreferencesDataStore.themeMode.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }
        }

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { themePreferencesDataStore.setThemeMode(mode) }
        }

        // AuthRepository.isAuthenticated derives from the same token store AuthRepository.signOut()
        // clears, so this reactively flips the app back to the auth screen (see RepoSwipeNavHost) —
        // no separate "signed out" event needs to be threaded back out of this screen. Going through
        // AuthRepository (rather than clearing TokenDataStore directly) also signs out of Firebase,
        // keeping its session in sync with the local one.
        fun signOut() {
            viewModelScope.launch { authRepository.signOut() }
        }
    }
