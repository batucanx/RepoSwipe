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

    /** Selecting the already-selected language clears it back to "no language filter". */
    fun selectLanguage(language: String)

    /** Sets the quick language explicitly. `null` is the unfiltered "For you" state. */
    fun setLanguage(language: String?)

    /** Selecting the already-selected topic clears it back to "no topic filter". */
    fun selectTopic(topic: String)

    fun setMinStars(stars: Int)

    fun setArchived(archived: Boolean)

    fun reset()
}

@Singleton
class DiscoverFilterRepositoryImpl
    @Inject
    constructor() : DiscoverFilterRepository {
        private val _filters = MutableStateFlow(DiscoverFilters())
        override val filters: StateFlow<DiscoverFilters> = _filters.asStateFlow()

        override fun selectLanguage(language: String) {
            _filters.update { it.copy(language = if (it.language == language) null else language) }
        }

        override fun setLanguage(language: String?) {
            _filters.update { filters ->
                if (filters.language == language) filters else filters.copy(language = language)
            }
        }

        override fun selectTopic(topic: String) {
            _filters.update { it.copy(topic = if (it.topic == topic) null else topic) }
        }

        override fun setMinStars(stars: Int) {
            _filters.update { it.copy(minStars = stars) }
        }

        override fun setArchived(archived: Boolean) {
            _filters.update { it.copy(archived = archived) }
        }

        override fun reset() {
            _filters.value = DiscoverFilters()
        }
    }
