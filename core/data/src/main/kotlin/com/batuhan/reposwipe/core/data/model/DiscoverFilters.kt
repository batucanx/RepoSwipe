package com.batuhan.reposwipe.core.data.model

/** "Any of" (OR) within each field — narrower fields still AND together in the search query. */
data class DiscoverFilters(
    val languages: Set<String> = emptySet(),
    val topics: Set<String> = emptySet(),
    val minStars: Int = 0,
    val updatedRecently: Boolean = false,
)
