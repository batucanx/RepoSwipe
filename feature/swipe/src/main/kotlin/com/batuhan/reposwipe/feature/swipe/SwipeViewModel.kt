package com.batuhan.reposwipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.model.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

val DiscoverLanguages = listOf("TypeScript", "Rust", "Python", "Go")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SwipeViewModel @Inject constructor(
    repoRepository: RepoRepository,
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    val repos: Flow<PagingData<Repo>> = _selectedLanguage
        .flatMapLatest { language -> repoRepository.searchRepos(language) }
        .cachedIn(viewModelScope)

    /**
     * The swipe deck's "cursor" into the paged results — swiping advances it, rewinding steps
     * it back. Undo is just decrementing this: Paging keeps already-loaded pages cached, so the
     * previous repo is still there to show again.
     */
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    fun selectLanguage(language: String) {
        _selectedLanguage.value = if (_selectedLanguage.value == language) null else language
        _currentIndex.value = 0
    }

    fun onSwiped(direction: SwipeDirection) {
        _currentIndex.value += 1
        // Faz 5: direction == SwipeDirection.Right triggers the actual GitHub star request.
    }

    fun onRewind() {
        _currentIndex.value = (_currentIndex.value - 1).coerceAtLeast(0)
    }
}
