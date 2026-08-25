package com.batuhan.reposwipe.feature.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FilterViewModel
    @Inject
    constructor(
        private val discoverFilterRepository: DiscoverFilterRepository,
    ) : ViewModel() {
        val uiState: StateFlow<FilterUiState> =
            discoverFilterRepository.filters
                .map { FilterUiState(filters = it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilterUiState())

        fun selectLanguage(language: String) = discoverFilterRepository.selectLanguage(language)

        fun selectTopic(topic: String) = discoverFilterRepository.selectTopic(topic)

        fun setMinStars(stars: Int) = discoverFilterRepository.setMinStars(stars)

        fun setArchived(archived: Boolean) = discoverFilterRepository.setArchived(archived)

        fun reset() = discoverFilterRepository.reset()
    }
