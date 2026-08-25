package com.batuhan.reposwipe.feature.followers

import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.designsystem.component.UserListItemData

enum class PeopleTab { FOLLOWERS, FOLLOWING }

data class PeopleUiState(
    val selectedTab: PeopleTab = PeopleTab.FOLLOWERS,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: UiText? = null,
    val followers: List<UserListItemData> = emptyList(),
    val following: List<UserListItemData> = emptyList(),
    val followersHasMore: Boolean = true,
    val followingHasMore: Boolean = true,
)
