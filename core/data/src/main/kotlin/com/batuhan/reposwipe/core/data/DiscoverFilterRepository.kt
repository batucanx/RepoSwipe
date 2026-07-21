package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the Discover search filters — shared between the Discover screen's
 * own quick-filter chip row and the dedicated Filter screen reached via the funnel icon, so
 * toggling a language from either place stays in sync everywhere.
 */
interface DiscoverFilterRepository {
    val filters: StateFlow<DiscoverFilters>

    fun toggleLanguage(language: String)

    fun toggleTopic(topic: String)

    fun setMinStars(stars: Int)

    fun setUpdatedRecently(updatedRecently: Boolean)

    fun reset()
}

@Singleton
class DiscoverFilterRepositoryImpl
    @Inject
    constructor() : DiscoverFilterRepository {
        private val _filters = MutableStateFlow(DiscoverFilters())
        override val filters: StateFlow<DiscoverFilters> = _filters.asStateFlow()

        override fun toggleLanguage(language: String) {
            _filters.update { it.copy(languages = it.languages.toggled(language)) }
        }

        override fun toggleTopic(topic: String) {
            _filters.update { it.copy(topics = it.topics.toggled(topic)) }
        }

        override fun setMinStars(stars: Int) {
            _filters.update { it.copy(minStars = stars) }
        }

        override fun setUpdatedRecently(updatedRecently: Boolean) {
            _filters.update { it.copy(updatedRecently = updatedRecently) }
        }

        override fun reset() {
            _filters.value = DiscoverFilters()
        }

        private fun Set<String>.toggled(value: String): Set<String> = if (value in this) this - value else this + value
    }
