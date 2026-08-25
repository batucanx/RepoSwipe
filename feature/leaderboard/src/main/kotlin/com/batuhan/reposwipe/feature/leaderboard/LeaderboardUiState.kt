package com.batuhan.reposwipe.feature.leaderboard

import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.model.LeaderboardEntry

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: UiText? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val hasMore: Boolean = true,
)
