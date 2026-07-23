package com.batuhan.reposwipe.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel
    @Inject
    constructor(
        private val leaderboardRepository: LeaderboardRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LeaderboardUiState())
        val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

        init {
            load(showFullScreenLoading = true)
        }

        fun retry() = load(showFullScreenLoading = true)

        fun refresh() = load(showFullScreenLoading = false)

        fun loadMore() {
            val state = _uiState.value
            if (state.isLoadingMore || !state.hasMore) return

            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMore = true) }
                val result = runCatching { leaderboardRepository.getLeaderboardPage(reset = false) }
                _uiState.update { current ->
                    val newEntries = result.getOrDefault(emptyList())
                    current.copy(
                        isLoadingMore = false,
                        entries = current.entries + newEntries,
                        hasMore = newEntries.size >= LeaderboardRepository.PAGE_SIZE,
                    )
                }
            }
        }

        private fun load(showFullScreenLoading: Boolean) {
            viewModelScope.launch {
                _uiState.update {
                    if (showFullScreenLoading) {
                        it.copy(isLoading = true, error = null)
                    } else {
                        it.copy(isRefreshing = true, error = null)
                    }
                }
                val result = runCatching { leaderboardRepository.getLeaderboardPage(reset = true) }
                result.exceptionOrNull()?.let { Sentry.captureException(it) }
                val entries = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        entries = entries,
                        hasMore = entries.size >= LeaderboardRepository.PAGE_SIZE,
                        error = if (result.isFailure) UiText.Resource(R.string.leaderboard_error_message) else null,
                    )
                }
            }
        }
    }
