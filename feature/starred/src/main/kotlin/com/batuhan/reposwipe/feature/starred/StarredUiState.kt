package com.batuhan.reposwipe.feature.starred

import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User

data class StarredUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val repos: List<Repo> = emptyList(),
    val loadedCount: Int = 0,
    val availableLanguages: List<String> = emptyList(),
    val hasMore: Boolean = true,
)
