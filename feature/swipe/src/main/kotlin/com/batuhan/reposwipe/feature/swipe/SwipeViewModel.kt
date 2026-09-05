package com.batuhan.reposwipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.Contributor
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.network.RateLimitInfo
import com.batuhan.reposwipe.core.network.RateLimitObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [Loaded.html] is null when the repo simply has no README file — a normal state, not an error.
 * It's GitHub's own already-rendered README HTML, ready to load into a WebView as-is. */
sealed interface ReadmeUiState {
    data object Loading : ReadmeUiState

    data class Loaded(
        val html: String?,
    ) : ReadmeUiState

    data object Error : ReadmeUiState
}

sealed interface LanguageBreakdownUiState {
    data object Loading : LanguageBreakdownUiState

    data class Loaded(
        val languages: Map<String, Long>,
    ) : LanguageBreakdownUiState

    data object Error : LanguageBreakdownUiState
}

sealed interface ContributorsUiState {
    data object Loading : ContributorsUiState

    data class Loaded(
        val contributors: List<Contributor>,
    ) : ContributorsUiState

    data object Error : ContributorsUiState
}

sealed interface SimilarReposUiState {
    data object Loading : SimilarReposUiState

    data class Loaded(
        val repos: List<Repo>,
    ) : SimilarReposUiState

    data object Error : SimilarReposUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SwipeViewModel
    @Inject
    constructor(
        private val repoRepository: RepoRepository,
        private val starRepository: StarRepository,
        private val leaderboardRepository: LeaderboardRepository,
        private val discoverFilterRepository: DiscoverFilterRepository,
        rateLimitObserver: RateLimitObserver,
    ) : ViewModel() {
        val filters: StateFlow<DiscoverFilters> = discoverFilterRepository.filters

        val repos: Flow<PagingData<Repo>> =
            filters
                .flatMapLatest { filters -> repoRepository.searchRepos(filters) }
                .cachedIn(viewModelScope)

        /**
         * The swipe deck's "cursor" into the paged results — swiping advances it, rewinding steps
         * it back. Undo is just decrementing this: Paging keeps already-loaded pages cached, so the
         * previous repo is still there to show again. It does not un-queue an already-enqueued star.
         */
        private val _currentIndex = MutableStateFlow(0)
        val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

        val rateLimit: StateFlow<RateLimitInfo?> = rateLimitObserver.state

        // Repo ids already handed to prefetchReadme this session — not a cache of the HTML itself
        // (that's OkHttp's disk cache, see NetworkModule.provideHttpCache), just a dedupe guard so
        // re-swiping past the same repo (rewind, or the deck's own peek window overlapping across
        // consecutive calls) doesn't fire a redundant network round trip for something already
        // in flight or already on disk.
        private val prefetchedReadmeRepoIds = mutableSetOf<Long>()

        init {
            // Any filter change (from this screen's quick-chip row or the full Filter screen)
            // invalidates the current swipe position — the underlying result set just changed.
            filters.onEach { _currentIndex.value = 0 }.launchIn(viewModelScope)
        }

        fun onSwiped(
            repo: Repo,
            direction: SwipeDirection,
        ) {
            _currentIndex.value += 1
            if (direction == SwipeDirection.Right) {
                viewModelScope.launch {
                    // A failure here means the star silently didn't persist — worth knowing about,
                    // unlike the best-effort leaderboard aggregate below.
                    runCatching { starRepository.starRepo(repo.ownerLogin, repo.name) }
                        .onFailure { Sentry.captureException(it) }
                }
                viewModelScope.launch {
                    runCatching { leaderboardRepository.recordSwipe(repo) }
                }
            }
        }

        fun onRewind() {
            _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
        }

        /** Quick category tabs are single-select; selecting the active tab is intentionally a no-op. */
        fun setQuickLanguage(language: String?) {
            discoverFilterRepository.setLanguage(language)
        }

        /**
         * Warms the OkHttp disk cache for a repo the deck currently has loaded or peeking, well
         * before its detail sheet is ever opened — called from [com.batuhan.reposwipe.feature.swipe.SwipeScreen]
         * for the deck's whole visible window (front card plus the up-to-2 peeking behind it) every
         * time that window shifts, i.e. on every swipe. GitHub sends `ETag`/`Cache-Control` on this
         * response, so by the time the user actually taps into one of these repos, [RepoDetailViewModel]'s
         * real fetch is a disk hit or a 304 instead of a full network round trip — the difference
         * between the detail screen's README appearing instantly and a visible loading spinner.
         *
         * Its failures are silently dropped — a failed prefetch just means the detail screen's own
         * load pays the normal cost later, exactly as if this had never run.
         */
        fun prefetchReadme(repo: Repo) {
            if (!prefetchedReadmeRepoIds.add(repo.id)) return
            viewModelScope.launch {
                runCatching { repoRepository.getReadmeHtml(repo.ownerLogin, repo.name) }
            }
        }
    }
