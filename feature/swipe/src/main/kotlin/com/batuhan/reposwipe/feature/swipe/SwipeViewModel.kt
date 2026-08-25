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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

val DiscoverLanguages = listOf("TypeScript", "Rust", "Python", "Go")

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

        private val _readmeState = MutableStateFlow<ReadmeUiState>(ReadmeUiState.Loading)
        val readmeState: StateFlow<ReadmeUiState> = _readmeState.asStateFlow()
        private var readmeJob: Job? = null

        private val _languageBreakdownState = MutableStateFlow<LanguageBreakdownUiState>(LanguageBreakdownUiState.Loading)
        val languageBreakdownState: StateFlow<LanguageBreakdownUiState> = _languageBreakdownState.asStateFlow()
        private var languageBreakdownJob: Job? = null

        private val _contributorsState = MutableStateFlow<ContributorsUiState>(ContributorsUiState.Loading)
        val contributorsState: StateFlow<ContributorsUiState> = _contributorsState.asStateFlow()
        private var contributorsJob: Job? = null

        private val _similarReposState = MutableStateFlow<SimilarReposUiState>(SimilarReposUiState.Loading)
        val similarReposState: StateFlow<SimilarReposUiState> = _similarReposState.asStateFlow()
        private var similarReposJob: Job? = null

        private val _detailRepo = MutableStateFlow<Repo?>(null)

        /** What GitHub last told us about the open repo; null until that call lands (or if it fails). */
        private val _detailServerStarred = MutableStateFlow<Boolean?>(null)
        private var starStateJob: Job? = null

        /**
         * Star state for the repo whose detail sheet is open — null while unknown, so the button can
         * stay neutral rather than falsely claiming "not starred". A still-pending local toggle wins
         * over the server's answer, so the icon flips the instant it's tapped.
         */
        val detailStarred: StateFlow<Boolean?> =
            combine(
                _detailRepo,
                _detailServerStarred,
                starRepository.observePendingStarStates(),
            ) { repo, serverStarred, pending ->
                repo?.let { pending["${it.ownerLogin}/${it.name}"] ?: serverStarred }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        init {
            // Any filter change (from this screen's quick-chip row or the full Filter screen)
            // invalidates the current swipe position — the underlying result set just changed.
            filters.onEach { _currentIndex.value = 0 }.launchIn(viewModelScope)
        }

        fun selectLanguage(language: String) {
            discoverFilterRepository.selectLanguage(language)
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

        /** Cancels any load already in flight for a previously opened repo, so a slow response for
         * one repo can never clobber the state of whichever repo's sheet is open now. */
        fun loadReadme(repo: Repo) {
            readmeJob?.cancel()
            _readmeState.value = ReadmeUiState.Loading
            readmeJob =
                viewModelScope.launch {
                    repoRepository
                        .getReadmeHtml(repo.ownerLogin, repo.name)
                        .onSuccess { html -> _readmeState.value = ReadmeUiState.Loaded(html) }
                        .onFailure { _readmeState.value = ReadmeUiState.Error }
                }
        }

        /** Same cancel-the-previous-load reasoning as [loadReadme]. */
        fun loadLanguageBreakdown(repo: Repo) {
            languageBreakdownJob?.cancel()
            _languageBreakdownState.value = LanguageBreakdownUiState.Loading
            languageBreakdownJob =
                viewModelScope.launch {
                    repoRepository
                        .getLanguageBreakdown(repo.ownerLogin, repo.name)
                        .onSuccess { languages -> _languageBreakdownState.value = LanguageBreakdownUiState.Loaded(languages) }
                        .onFailure { _languageBreakdownState.value = LanguageBreakdownUiState.Error }
                }
        }

        /** Same cancel-the-previous-load reasoning as [loadReadme]. */
        fun loadContributors(repo: Repo) {
            contributorsJob?.cancel()
            _contributorsState.value = ContributorsUiState.Loading
            contributorsJob =
                viewModelScope.launch {
                    repoRepository
                        .getContributors(repo.ownerLogin, repo.name)
                        .onSuccess { contributors -> _contributorsState.value = ContributorsUiState.Loaded(contributors) }
                        .onFailure { _contributorsState.value = ContributorsUiState.Error }
                }
        }

        /** Same cancel-the-previous-load reasoning as [loadReadme]. */
        fun loadSimilarRepos(repo: Repo) {
            similarReposJob?.cancel()
            _similarReposState.value = SimilarReposUiState.Loading
            similarReposJob =
                viewModelScope.launch {
                    repoRepository
                        .getSimilarRepos(repo)
                        .onSuccess { repos -> _similarReposState.value = SimilarReposUiState.Loaded(repos) }
                        .onFailure { _similarReposState.value = SimilarReposUiState.Error }
                }
        }

        /** Same cancel-the-previous-load reasoning as [loadReadme]. */
        fun onDetailOpened(repo: Repo) {
            loadReadme(repo)
            loadLanguageBreakdown(repo)
            loadContributors(repo)
            loadSimilarRepos(repo)
            starStateJob?.cancel()
            _detailRepo.value = repo
            _detailServerStarred.value = null
            starStateJob =
                viewModelScope.launch {
                    _detailServerStarred.value =
                        runCatching { starRepository.isStarred(repo.ownerLogin, repo.name) }
                            .onFailure { Sentry.captureException(it) }
                            .getOrNull()
                }
        }

        fun onDetailClosed() {
            starStateJob?.cancel()
            languageBreakdownJob?.cancel()
            contributorsJob?.cancel()
            similarReposJob?.cancel()
            _detailRepo.value = null
            _detailServerStarred.value = null
        }

        /**
         * The real GitHub star, independent of the swipe deck — this is what makes a repo's detail
         * sheet a place you can star from, rather than only being able to star by swiping the card.
         */
        fun toggleDetailStar() {
            val repo = _detailRepo.value ?: return
            val currentlyStarred = detailStarred.value ?: false
            viewModelScope.launch {
                runCatching {
                    if (currentlyStarred) {
                        starRepository.unstarRepo(repo.ownerLogin, repo.name)
                    } else {
                        starRepository.starRepo(repo.ownerLogin, repo.name)
                    }
                }.onFailure { Sentry.captureException(it) }
            }
        }
    }
