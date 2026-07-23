package com.batuhan.reposwipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(
        val isAuthenticated: Boolean,
    ) : MainActivityUiState
}

/**
 * Keeps collecting [AuthRepository.isAuthenticated] for the activity's lifetime, not just
 * once at startup, so a token invalidated mid-session (e.g. AuthInterceptor clearing it after
 * a 401) is reflected here too and [com.batuhan.reposwipe.navigation.RepoSwipeNavHost] can
 * react by sending the user back to the auth screen.
 */
@HiltViewModel
class MainActivityViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val uiState: StateFlow<MainActivityUiState> =
            authRepository.isAuthenticated
                .map { MainActivityUiState.Success(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = MainActivityUiState.Loading,
                )
    }
