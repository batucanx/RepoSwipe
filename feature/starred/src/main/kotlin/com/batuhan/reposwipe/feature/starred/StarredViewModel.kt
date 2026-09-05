package com.batuhan.reposwipe.feature.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.StarredReposRepository
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.data.model.ownerRepoKey
import com.batuhan.reposwipe.core.data.model.ownerRepoKeyParts
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A pending-star outbox key's stand-in [Repo] fetch: still loading, or resolved. */
private sealed interface PendingStarEntry {
    data object Loading : PendingStarEntry

    data class Resolved(
        val repo: Repo,
    ) : PendingStarEntry
}

private data class FetchState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: UiText? = null,
    val transientError: UiText? = null,
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
        private val repoRepository: RepoRepository,
    ) : ViewModel() {
        private val _fetchState = MutableStateFlow(FetchState())
        private val _selectedLanguage = MutableStateFlow<String?>(null)
        val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

        // Star/unstar goes through a local outbox synced in the background (see
        // StarRepository/StarSyncWorker) rather than hitting GitHub inline, so a repo just starred
        // elsewhere (Discover, Leaderboard, Search) wouldn't show up here until that background sync
        // finished *and* this screen re-fetched — a visible, sometimes multi-second lag. This map
        // holds one entry per "owner/repo" the outbox still has a pending *star* for: [PendingStarEntry.Loading]
        // while [repoRepository] (Room-cached from wherever the user first saw the repo, falling back
        // to network) is still fetching a stand-in [Repo], then [PendingStarEntry.Resolved] once it
        // lands — a single map instead of a separate "resolved" map plus an "in flight" set means
        // there's only one place a key's state can live, so the two can't drift out of sync with each
        // other. Only [PendingStarEntry.Resolved] entries are merged into [uiState] below. Entries are
        // dropped once a real fetch confirms them (see the collector in `init`) — a pending *unstar*
        // superseding one of these is handled for free by the existing `pendingUnstars` filter already
        // in the `combine` below, since it runs on the merged list.
        private val _pendingStarRepos = MutableStateFlow<Map<String, PendingStarEntry>>(emptyMap())

        private var currentPage = 0
        private var loadJob: Job? = null
        private var loadMoreJob: Job? = null

        val uiState: StateFlow<StarredUiState> =
            combine(
                _fetchState,
                starRepository.observePendingUnstars(),
                _selectedLanguage,
                _pendingStarRepos,
            ) { fetch, pendingUnstars, language, pendingStars ->
                val serverKeys = fetch.serverRepos.mapTo(mutableSetOf(), Repo::ownerRepoKey)
                val notYetOnServer =
                    pendingStars
                        .filterKeys { it !in serverKeys }
                        .values
                        .filterIsInstance<PendingStarEntry.Resolved>()
                        .map { it.repo }
                val combined = notYetOnServer + fetch.serverRepos
                val notUnstarred = combined.filterNot { it.ownerRepoKey in pendingUnstars }
                StarredUiState(
                    isLoading = fetch.isLoading,
                    isRefreshing = fetch.isRefreshing,
                    isLoadingMore = fetch.isLoadingMore,
                    error = fetch.error,
                    transientError = fetch.transientError,
                    user = fetch.user,
                    repos = notUnstarred.filter { language == null || it.language == language },
                    loadedCount = notUnstarred.size,
                    availableLanguages = notUnstarred.mapNotNull { it.language }.distinct().take(MAX_LANGUAGE_TABS),
                    hasMore = fetch.hasMore,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StarredUiState())

        init {
            load(showFullScreenLoading = true)
            observePendingStars()
        }

        /**
         * Keeps [_pendingStarRepos] in sync with the outbox: fetches a [Repo] for every newly
         * pending-star key not already tracked (loading or resolved), and evicts entries once
         * [_fetchState]'s own server list catches up to them for real.
         *
         * Keyed only on `serverRepos` (via `distinctUntilChanged`) rather than the raw [_fetchState],
         * since that's the only part of it this logic reads — otherwise every unrelated field flip
         * (`isLoading`, `isLoadingMore`, `transientError`, ...) during a load/refresh/loadMore would
         * re-run this whole eviction/fetch pass for nothing.
         */
        private fun observePendingStars() {
            viewModelScope.launch {
                combine(
                    _fetchState.map { it.serverRepos }.distinctUntilChanged(),
                    starRepository.observePendingStarStates(),
                ) { serverRepos, states -> serverRepos to states }
                    .collect { (serverRepos, states) ->
                        val serverKeys = serverRepos.mapTo(mutableSetOf(), Repo::ownerRepoKey)
                        val pendingStarKeys = states.filterValues { it }.keys

                        // The outbox settled these keys (synced, or gave up after a non-retryable
                        // failure — see StarSyncWorker) since we started tracking them, but our own
                        // server snapshot hasn't caught up yet. Evict and refresh rather than leaving
                        // eviction only-on-success: otherwise a star GitHub ultimately rejected would
                        // stay shown as starred forever, since it can never appear in serverKeys.
                        val settledButNotYetOnServer = _pendingStarRepos.value.keys - serverKeys - pendingStarKeys
                        if (settledButNotYetOnServer.isNotEmpty()) {
                            _pendingStarRepos.update { it - settledButNotYetOnServer }
                            refresh()
                        }
                        _pendingStarRepos.update { current -> current.filterKeys { it !in serverKeys } }

                        val toFetch = pendingStarKeys - serverKeys - _pendingStarRepos.value.keys
                        toFetch.forEach { key ->
                            val (ownerLogin, repoName) = ownerRepoKeyParts(key) ?: return@forEach
                            _pendingStarRepos.update { it + (key to PendingStarEntry.Loading) }
                            launch {
                                val repo = repoRepository.getRepository(ownerLogin, repoName).getOrNull()
                                _pendingStarRepos.update { current ->
                                    if (repo != null) {
                                        current + (key to PendingStarEntry.Resolved(repo))
                                    } else {
                                        current - key
                                    }
                                }
                            }
                        }
                    }
            }
        }

        fun retry() = load(showFullScreenLoading = true)

        fun refresh() = load(showFullScreenLoading = false)

        fun consumeTransientError() {
            _fetchState.update { it.copy(transientError = null) }
        }

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
            if (loadMoreJob?.isActive == true || !state.canLoadMore()) {
                return
            }

            loadMoreJob =
                viewModelScope.launch {
                    _fetchState.update { it.copy(isLoadingMore = true, transientError = null) }
                    val nextPage = currentPage + 1
                    try {
                        suspendResult { starredReposRepository.getStarredReposPage(page = nextPage) }
                            .onSuccess { newItems ->
                                _fetchState.update { current ->
                                    current.copy(
                                        serverRepos = (current.serverRepos + newItems).distinctBy(Repo::id),
                                        hasMore = newItems.size >= PAGE_SIZE,
                                    )
                                }
                                currentPage = nextPage
                            }.onFailure { error ->
                                Sentry.captureException(error)
                                _fetchState.update {
                                    it.copy(transientError = UiText.Resource(R.string.starred_error_message))
                                }
                            }
                    } finally {
                        _fetchState.update { it.copy(isLoadingMore = false) }
                    }
                }
        }

        private fun FetchState.canLoadMore(): Boolean =
            !isLoading &&
                !isRefreshing &&
                !isLoadingMore &&
                hasMore

        private fun load(showFullScreenLoading: Boolean) {
            if (loadJob?.isActive == true) return
            loadMoreJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    val previous = _fetchState.value
                    val showBlockingLoading = showFullScreenLoading && previous.serverRepos.isEmpty()
                    _fetchState.update {
                        if (showBlockingLoading) {
                            it.copy(isLoading = true, isLoadingMore = false, error = null, transientError = null)
                        } else {
                            it.copy(isRefreshing = true, isLoadingMore = false, transientError = null)
                        }
                    }

                    try {
                        val userRequest =
                            if (previous.user == null) {
                                async { suspendResult { userRepository.getCurrentUser() } }
                            } else {
                                null
                            }
                        val reposRequest = async { suspendResult { starredReposRepository.getStarredReposPage(page = 1) } }
                        val reposResult = reposRequest.await()
                        val userResult = userRequest?.await()

                        userResult?.exceptionOrNull()?.let(Sentry::captureException)
                        reposResult.exceptionOrNull()?.let(Sentry::captureException)

                        if (reposResult.isSuccess) {
                            val repos = reposResult.getOrThrow()
                            _fetchState.update {
                                it.copy(
                                    user = userResult?.getOrNull() ?: it.user,
                                    serverRepos = repos,
                                    hasMore = repos.size >= PAGE_SIZE,
                                    error = null,
                                )
                            }
                            currentPage = 1
                        } else {
                            _fetchState.update {
                                if (it.serverRepos.isEmpty()) {
                                    it.copy(error = UiText.Resource(R.string.starred_error_message))
                                } else {
                                    it.copy(transientError = UiText.Resource(R.string.starred_error_message))
                                }
                            }
                        }
                    } finally {
                        _fetchState.update { it.copy(isLoading = false, isRefreshing = false) }
                    }
                }
        }

        private companion object {
            const val PAGE_SIZE = 30
            const val MAX_LANGUAGE_TABS = 5
        }
    }

// Retrofit/converter failures share no narrower common type than Exception. Cancellation remains
// structural control flow and is always rethrown before converting operational failures to Result.
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> suspendResult(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
