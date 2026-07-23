package com.batuhan.reposwipe.feature.filter

import com.batuhan.reposwipe.core.data.model.DiscoverFilters

data class FilterTopic(
    val label: String,
    val slug: String,
)

val AvailableLanguages =
    listOf("JavaScript", "Python", "Rust", "Go", "Java", "TypeScript", "Kotlin", "Swift", "C++", "Ruby")

val AvailableTopics =
    listOf(
        FilterTopic("Machine Learning", "machine-learning"),
        FilterTopic("Web Development", "web-development"),
        FilterTopic("Mobile", "mobile"),
        FilterTopic("Blockchain", "blockchain"),
        FilterTopic("DevOps", "devops"),
        FilterTopic("Open Source", "open-source"),
    )

const val MAX_STARS = 50_000
const val MIN_STARS_FLOOR = 0
const val STAR_STEP = 500

data class FilterUiState(
    val filters: DiscoverFilters = DiscoverFilters(),
)
