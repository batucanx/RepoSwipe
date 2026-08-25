package com.batuhan.reposwipe.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
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
                result.exceptionOrNull()?.let { Sentry.captureException(it) }
                // Recent repos are a supplementary section — a failure here shouldn't block or
                // error out the rest of the profile, so it's swallowed to an empty list.
                val recentRepos =
                    runCatching { userRepository.getRecentRepos(RECENT_REPOS_LIMIT) }
                        .onFailure { Sentry.captureException(it) }
                        .getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = result.getOrNull(),
                        recentRepos = recentRepos,
                        error = if (result.isFailure) UiText.Resource(R.string.profile_error_message) else null,
                    )
                }
            }
        }

        private companion object {
            const val RECENT_REPOS_LIMIT = 4
        }
    }
