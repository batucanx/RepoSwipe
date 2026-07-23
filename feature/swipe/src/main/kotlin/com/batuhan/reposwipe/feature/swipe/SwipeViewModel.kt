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

val DiscoverLanguages = listOf("TypeScript", "Rust", "Python", "Go")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SwipeViewModel
    @Inject
    constructor(
        repoRepository: RepoRepository,
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

        init {
            // Any filter change (from this screen's quick-chip row or the full Filter screen)
            // invalidates the current swipe position — the underlying result set just changed.
            filters.onEach { _currentIndex.value = 0 }.launchIn(viewModelScope)
        }

        fun toggleLanguage(language: String) {
            discoverFilterRepository.toggleLanguage(language)
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
    }
