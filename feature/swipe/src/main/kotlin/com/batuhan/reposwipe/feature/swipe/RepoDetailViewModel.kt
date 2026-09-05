package com.batuhan.reposwipe.feature.swipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.ownerRepoKey
import com.batuhan.reposwipe.feature.swipe.navigation.REPO_NAME_ARG
import com.batuhan.reposwipe.feature.swipe.navigation.REPO_OWNER_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoDetailUiState(
    val repo: Repo? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val readme: ReadmeUiState = ReadmeUiState.Loading,
    val languageBreakdown: LanguageBreakdownUiState = LanguageBreakdownUiState.Loading,
    val contributors: ContributorsUiState = ContributorsUiState.Loading,
    val similarRepos: SimilarReposUiState = SimilarReposUiState.Loading,
)

@HiltViewModel
class RepoDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repoRepository: RepoRepository,
        private val starRepository: StarRepository,
    ) : ViewModel() {
        private val owner = requireNotNull(savedStateHandle.get<String>(REPO_OWNER_ARG))
        private val name = requireNotNull(savedStateHandle.get<String>(REPO_NAME_ARG))

        private val _uiState = MutableStateFlow(RepoDetailUiState())
        val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

        private val _serverStarred = MutableStateFlow<Boolean?>(null)
        val isStarred: StateFlow<Boolean?> =
            combine(
                _uiState,
                _serverStarred,
                starRepository.observePendingStarStates(),
            ) { state, serverStarred, pending ->
                state.repo?.let { repo -> pending[repo.ownerRepoKey] ?: serverStarred }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private var repoJob: Job? = null
        private var readmeJob: Job? = null
        private var languagesJob: Job? = null
        private var contributorsJob: Job? = null
        private var similarReposJob: Job? = null
        private var starStateJob: Job? = null

        init {
            loadRepository()
        }

        fun retry() {
            loadRepository()
        }

        fun retryReadme() {
            val repo = _uiState.value.repo ?: return
            loadReadme(repo)
        }

        fun toggleStar() {
            val repo = _uiState.value.repo ?: return
            val currentlyStarred = isStarred.value ?: return
            viewModelScope.launch {
                runCatching {
                    if (currentlyStarred) {
                        starRepository.unstarRepo(repo.ownerLogin, repo.name)
                    } else {
                        starRepository.starRepo(repo.ownerLogin, repo.name)
                    }
                }.onFailure(Sentry::captureException)
            }
        }

        private fun loadRepository() {
            repoJob?.cancel()
            cancelSectionJobs()
            _uiState.value = RepoDetailUiState()
            _serverStarred.value = null
            repoJob =
                viewModelScope.launch {
                    repoRepository
                        .getRepository(owner, name)
                        .onSuccess { repo ->
                            _uiState.update { it.copy(repo = repo, isLoading = false, hasError = false) }
                            loadSections(repo)
                        }.onFailure { throwable ->
                            Sentry.captureException(throwable)
                            _uiState.update { it.copy(isLoading = false, hasError = true) }
                        }
                }
        }

        private fun loadSections(repo: Repo) {
            loadReadme(repo)
            languagesJob =
                viewModelScope.launch {
                    repoRepository
                        .getLanguageBreakdown(repo.ownerLogin, repo.name)
                        .onSuccess { languages ->
                            _uiState.update { it.copy(languageBreakdown = LanguageBreakdownUiState.Loaded(languages)) }
                        }.onFailure {
                            _uiState.update { it.copy(languageBreakdown = LanguageBreakdownUiState.Error) }
                        }
                }
            contributorsJob =
                viewModelScope.launch {
                    repoRepository
                        .getContributors(repo.ownerLogin, repo.name)
                        .onSuccess { contributors ->
                            _uiState.update { it.copy(contributors = ContributorsUiState.Loaded(contributors)) }
                        }.onFailure {
                            _uiState.update { it.copy(contributors = ContributorsUiState.Error) }
                        }
                }
            similarReposJob =
                viewModelScope.launch {
                    repoRepository
                        .getSimilarRepos(repo)
                        .onSuccess { repos ->
                            _uiState.update { it.copy(similarRepos = SimilarReposUiState.Loaded(repos)) }
                        }.onFailure {
                            _uiState.update { it.copy(similarRepos = SimilarReposUiState.Error) }
                        }
                }
            starStateJob =
                viewModelScope.launch {
                    _serverStarred.value =
                        runCatching { starRepository.isStarred(repo.ownerLogin, repo.name) }
                            .onFailure(Sentry::captureException)
                            .getOrNull()
                }
        }

        private fun loadReadme(repo: Repo) {
            readmeJob?.cancel()
            _uiState.update { it.copy(readme = ReadmeUiState.Loading) }
            readmeJob =
                viewModelScope.launch {
                    repoRepository
                        .getReadmeHtml(repo.ownerLogin, repo.name)
                        .onSuccess { html -> _uiState.update { it.copy(readme = ReadmeUiState.Loaded(html)) } }
                        .onFailure { throwable ->
                            Sentry.captureException(throwable)
                            _uiState.update { it.copy(readme = ReadmeUiState.Error) }
                        }
                }
        }

        private fun cancelSectionJobs() {
            readmeJob?.cancel()
            languagesJob?.cancel()
            contributorsJob?.cancel()
            similarReposJob?.cancel()
            starStateJob?.cancel()
        }
    }
