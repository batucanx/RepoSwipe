package com.batuhan.reposwipe.feature.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.StarredReposRepository
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val user: User? = null,
    val serverRepos: List<Repo> = emptyList(),
    val hasMore: Boolean = true,
)

@HiltViewModel
class StarredViewModel
    @Inject
    constructor(
        private val starredReposRepository: StarredReposRepository,
        private val starRepository: StarRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _fetchState = MutableStateFlow(FetchState())
        private val _selectedLanguage = MutableStateFlow<String?>(null)
        val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

        private var currentPage = 0

        val uiState: StateFlow<StarredUiState> =
            combine(
                _fetchState,
                starRepository.observePendingUnstars(),
                _selectedLanguage,
            ) { fetch, pendingUnstars, language ->
                val notUnstarred = fetch.serverRepos.filterNot { "${it.ownerLogin}/${it.name}" in pendingUnstars }
                StarredUiState(
                    isLoading = fetch.isLoading,
                    isRefreshing = fetch.isRefreshing,
                    isLoadingMore = fetch.isLoadingMore,
                    error = fetch.error,
                    user = fetch.user,
                    repos = notUnstarred.filter { language == null || it.language == language },
                    loadedCount = notUnstarred.size,
                    availableLanguages = notUnstarred.mapNotNull { it.language }.distinct().take(MAX_LANGUAGE_TABS),
                    hasMore = fetch.hasMore,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StarredUiState())

        init {
            load(showFullScreenLoading = true)
        }

        fun retry() = load(showFullScreenLoading = true)

        fun refresh() = load(showFullScreenLoading = false)

        fun selectLanguage(language: String?) {
            _selectedLanguage.value = language
        }

        /**
         * Removes [repo] from the visible list immediately and for good, rather than relying on
         * [StarRepository.observePendingUnstars] alone: that flow only hides a repo while its
         * unstar is still queued, so the instant [StarSyncWorker][com.batuhan.reposwipe.core.data.StarSyncWorker]
         * finishes syncing it (each queued entry is spaced ~1s apart, deliberately, to avoid
         * hammering GitHub) and clears the outbox row, the repo would reappear here — `serverRepos`
         * is a point-in-time snapshot from the last load/refresh and was never told the star was
         * removed. That reappearance was most visible when unstarring several repos in quick
         * succession: the ones synced first would pop back into the list while later ones were
         * still mid-flight. Mutating `serverRepos` directly sidesteps the outbox's timing entirely.
         */
        fun unstar(repo: Repo) {
            _fetchState.update { it.copy(serverRepos = it.serverRepos.filterNot { r -> r.id == repo.id }) }
            viewModelScope.launch {
                starRepository.unstarRepo(repo.ownerLogin, repo.name)
            }
        }

        fun loadMore() {
            val state = _fetchState.value
            if (state.isLoadingMore || !state.hasMore) return

            viewModelScope.launch {
                _fetchState.update { it.copy(isLoadingMore = true) }
                val nextPage = currentPage + 1
                val result = runCatching { starredReposRepository.getStarredReposPage(page = nextPage) }
                _fetchState.update { current ->
                    val newItems = result.getOrDefault(emptyList())
                    current.copy(
                        isLoadingMore = false,
                        serverRepos = current.serverRepos + newItems,
                        hasMore = newItems.size >= PAGE_SIZE,
                    )
                }
                if (result.isSuccess) currentPage = nextPage
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
                val userResult = runCatching { userRepository.getCurrentUser() }
                val reposResult = runCatching { starredReposRepository.getStarredReposPage(page = 1) }
                userResult.exceptionOrNull()?.let { Sentry.captureException(it) }
                reposResult.exceptionOrNull()?.let { Sentry.captureException(it) }
                _fetchState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        user = userResult.getOrNull(),
                        serverRepos = reposResult.getOrDefault(emptyList()),
                        hasMore = reposResult.getOrDefault(emptyList()).size >= PAGE_SIZE,
                        error = if (reposResult.isFailure) UiText.Resource(R.string.starred_error_message) else null,
                    )
                }
                currentPage = 1
            }
        }

        private companion object {
            const val PAGE_SIZE = 30
            const val MAX_LANGUAGE_TABS = 5
        }
    }
