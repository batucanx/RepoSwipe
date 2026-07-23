package com.batuhan.reposwipe.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            refresh()
        }

        fun retry() = refresh()

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

        private fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val result = runCatching { leaderboardRepository.getLeaderboardPage(reset = true) }
                val entries = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        entries = entries,
                        hasMore = entries.size >= LeaderboardRepository.PAGE_SIZE,
                        error = if (result.isFailure) UiText.Resource(R.string.leaderboard_error_message) else null,
                    )
                }
            }
        }
    }
