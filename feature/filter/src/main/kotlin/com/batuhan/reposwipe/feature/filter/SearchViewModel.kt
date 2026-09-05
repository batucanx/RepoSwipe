package com.batuhan.reposwipe.feature.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.ownerRepoKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** GitHub repo search, reachable from Discover's top bar. Search only runs on explicit
 * [submit] (not live-as-you-type), same UX as GitHub's own search — avoids hammering the API on
 * every keystroke and matches [submittedQuery]'s use as "has the user actually searched yet". */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repoRepository: RepoRepository,
        private val starRepository: StarRepository,
    ) : ViewModel() {
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private val _submittedQuery = MutableStateFlow<String?>(null)
        val submittedQuery: StateFlow<String?> = _submittedQuery.asStateFlow()

        val repos: Flow<PagingData<Repo>> =
            _submittedQuery
                .flatMapLatest { term ->
                    if (term.isNullOrBlank()) flowOf(PagingData.empty()) else repoRepository.searchRepos(freeText = term)
                }.cachedIn(viewModelScope)

        /** Same "outbox-pending-only" reasoning as `LeaderboardViewModel.starredStates` — search
         * results don't batch-check GitHub's real starred state, only reflect taps made here. */
        val starredStates: StateFlow<Map<String, Boolean>> =
            starRepository
                .observePendingStarStates()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        fun onQueryChange(value: String) {
            _query.value = value
        }

        fun submit() {
            val term = _query.value.trim()
            if (term.isEmpty()) return
            _submittedQuery.value = term
        }

        fun toggleStar(repo: Repo) {
            val key = repo.ownerRepoKey
            val currentlyStarred = starredStates.value[key] == true
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
