package com.batuhan.reposwipe.feature.followers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.FollowRepository
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.designsystem.component.UserListItemData
import com.batuhan.reposwipe.feature.followers.navigation.PEOPLE_INITIAL_TAB_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class FetchState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: UiText? = null,
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val followersHasMore: Boolean = true,
    val followingHasMore: Boolean = true,
    /** Every login the authenticated user follows — cross-referenced against [followers] rows so
     * a Followers-tab row can show the right "Follow"/"Following" state without an N+1 API call. */
    val followingLogins: Set<String> = emptySet(),
)

@HiltViewModel
class PeopleViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val followRepository: FollowRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _fetchState = MutableStateFlow(FetchState())
        private val _selectedTab =
            MutableStateFlow(
                savedStateHandle.get<String>(PEOPLE_INITIAL_TAB_ARG)?.let { arg ->
                    runCatching { PeopleTab.valueOf(arg) }.getOrNull()
                } ?: PeopleTab.FOLLOWERS,
            )

        private var followersPage = 0
        private var followingPage = 0

        val uiState: StateFlow<PeopleUiState> =
            combine(
                _fetchState,
                followRepository.observePendingFollowStates(),
                _selectedTab,
            ) { fetch, pending, tab ->
                PeopleUiState(
                    selectedTab = tab,
                    isLoading = fetch.isLoading,
                    isRefreshing = fetch.isRefreshing,
                    isLoadingMore = fetch.isLoadingMore,
                    error = fetch.error,
                    followers =
                        fetch.followers.map {
                            it.toItemData(
                                baseFollowing = it.login in fetch.followingLogins,
                                pending = pending,
                            )
                        },
                    following =
                        fetch.following.map { it.toItemData(baseFollowing = true, pending = pending) },
                    followersHasMore = fetch.followersHasMore,
                    followingHasMore = fetch.followingHasMore,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleUiState())

        init {
            load(showFullScreenLoading = true)
        }

        fun retry() = load(showFullScreenLoading = true)

        fun refresh() = load(showFullScreenLoading = false)

        fun selectTab(tab: PeopleTab) {
            _selectedTab.value = tab
        }

        fun toggleFollow(user: UserListItemData) {
            val willFollow = !user.isFollowing
            viewModelScope.launch {
                if (user.isFollowing) {
                    followRepository.unfollow(user.login)
                } else {
                    followRepository.follow(user.login)
                }
            }
            // The Following tab's list only comes from a one-time fetch (page 1 on load/refresh) —
            // without this, a newly-followed user only shows up there after a manual refresh.
            // Update it (and the followers cross-reference set) optimistically, same spirit as
            // StarRepository.observePendingUnstars() hiding a pending unstar before sync confirms it.
            _fetchState.update { state ->
                val following =
                    if (willFollow) {
                        if (state.following.any { it.login == user.login }) {
                            state.following
                        } else {
                            state.following +
                                User(
                                    login = user.login,
                                    name = user.displayName,
                                    avatarUrl = user.avatarUrl,
                                    publicRepos = 0,
                                    followers = 0,
                                    following = 0,
                                )
                        }
                    } else {
                        state.following.filterNot { it.login == user.login }
                    }
                val followingLogins =
                    if (willFollow) state.followingLogins + user.login else state.followingLogins - user.login
                state.copy(following = following, followingLogins = followingLogins)
            }
        }

        fun loadMore() {
            val tab = _selectedTab.value
            val state = _fetchState.value
            if (state.isLoadingMore) return
            val hasMore = if (tab == PeopleTab.FOLLOWERS) state.followersHasMore else state.followingHasMore
            if (!hasMore) return

            viewModelScope.launch {
                _fetchState.update { it.copy(isLoadingMore = true) }
                when (tab) {
                    PeopleTab.FOLLOWERS -> {
                        val nextPage = followersPage + 1
                        val result = runCatching { followRepository.getFollowers(page = nextPage) }
                        _fetchState.update { current ->
                            val newItems = result.getOrDefault(emptyList())
                            current.copy(
                                isLoadingMore = false,
                                followers = current.followers + newItems,
                                followersHasMore = newItems.size >= FollowRepository.PAGE_SIZE,
                            )
                        }
                        if (result.isSuccess) followersPage = nextPage
                    }
                    PeopleTab.FOLLOWING -> {
                        val nextPage = followingPage + 1
                        val result = runCatching { followRepository.getFollowing(page = nextPage) }
                        _fetchState.update { current ->
                            val newItems = result.getOrDefault(emptyList())
                            current.copy(
                                isLoadingMore = false,
                                following = current.following + newItems,
                                followingHasMore = newItems.size >= FollowRepository.PAGE_SIZE,
                            )
                        }
                        if (result.isSuccess) followingPage = nextPage
                    }
                }
            }
        }

        private fun load(showFullScreenLoading: Boolean) {
            viewModelScope.launch {
                _fetchState.update {
                    if (showFullScreenLoading) {
                        it.copy(isLoading = true, error = null)
                    } else {
                        it.copy(isRefreshing = true, error = null)
                    }
                }

                val followersResult = runCatching { followRepository.getFollowers(page = 1) }
                val followingResult = runCatching { followRepository.getFollowing(page = 1) }
                val followingLoginsResult = runCatching { userRepository.getAllFollowingLogins() }
                listOf(followersResult, followingResult, followingLoginsResult)
                    .mapNotNull { it.exceptionOrNull() }
                    .forEach { Sentry.captureException(it) }

                _fetchState.update {
                    val followers = followersResult.getOrDefault(emptyList())
                    val following = followingResult.getOrDefault(emptyList())
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        followers = followers,
                        following = following,
                        followersHasMore = followers.size >= FollowRepository.PAGE_SIZE,
                        followingHasMore = following.size >= FollowRepository.PAGE_SIZE,
                        followingLogins = followingLoginsResult.getOrDefault(emptySet()),
                        // Any of the three calls failing silently produced a misleadingly "empty
                        // but fine" list before — surface an error for all of them, not just
                        // followers.
                        error =
                            if (followersResult.isFailure || followingResult.isFailure || followingLoginsResult.isFailure) {
                                UiText.Resource(R.string.people_error_message)
                            } else {
                                null
                            },
                    )
                }
                followersPage = 1
                followingPage = 1
            }
        }

        private fun User.toItemData(
            baseFollowing: Boolean,
            pending: Map<String, Boolean>,
        ): UserListItemData =
            UserListItemData(
                login = login,
                displayName = name,
                avatarUrl = avatarUrl,
                isFollowing = pending[login] ?: baseFollowing,
            )
    }
