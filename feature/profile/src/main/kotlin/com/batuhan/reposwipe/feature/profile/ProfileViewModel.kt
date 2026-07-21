package com.batuhan.reposwipe.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val tokenDataStore: TokenDataStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun retry() = refresh()

        fun signOut() {
            viewModelScope.launch {
                tokenDataStore.clearAccessToken()
                _uiState.update { it.copy(signedOut = true) }
            }
        }

        private fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val result = runCatching { userRepository.getCurrentUser() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = result.getOrNull(),
                        error = if (result.isFailure) "Profil yüklenemedi." else null,
                    )
                }
            }
        }
    }
