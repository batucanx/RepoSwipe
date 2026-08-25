package com.batuhan.reposwipe.feature.auth.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.datastore.OnboardingPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the short first-launch survey that seeds [DiscoverFilterRepository] with the new user's
 * interests instead of leaving Discover on an unfiltered default. The nav host only routes here
 * once per real sign-in (a cached-token relaunch skips straight past Auth); the persisted
 * `onboarding_completed` flag is a belt-and-suspenders guard against showing it again after a
 * sign-out/sign-in cycle.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val discoverFilterRepository: DiscoverFilterRepository,
        private val onboardingPreferencesDataStore: OnboardingPreferencesDataStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val alreadyCompleted = onboardingPreferencesDataStore.hasCompletedOnboarding.first()
                _uiState.update { it.copy(isLoading = false, finished = alreadyCompleted) }
            }
        }

        fun selectLanguage(language: String) {
            _uiState.update { it.copy(selectedLanguage = language, step = OnboardingStep.TOPIC) }
        }

        fun skipLanguage() {
            _uiState.update { it.copy(step = OnboardingStep.TOPIC) }
        }

        fun selectTopic(topic: String) {
            _uiState.update { it.copy(selectedTopic = topic, step = OnboardingStep.POPULARITY) }
        }

        fun skipTopic() {
            _uiState.update { it.copy(step = OnboardingStep.POPULARITY) }
        }

        /** Also used by the screen's persistent "skip all" action — whatever's been picked so far
         * (possibly nothing) is applied, then onboarding is marked complete either way. */
        fun finish(popularOnly: Boolean) {
            viewModelScope.launch {
                val state = _uiState.value
                state.selectedLanguage?.let(discoverFilterRepository::selectLanguage)
                state.selectedTopic?.let(discoverFilterRepository::selectTopic)
                if (popularOnly) discoverFilterRepository.setMinStars(POPULAR_MIN_STARS)
                onboardingPreferencesDataStore.setOnboardingCompleted()
                _uiState.update { it.copy(finished = true) }
            }
        }

        private companion object {
            const val POPULAR_MIN_STARS = 1000
        }
    }
