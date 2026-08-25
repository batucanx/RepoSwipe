package com.batuhan.reposwipe.core.data.model

/**
 * Single-select language/topic (not a `Set`): GitHub's repository search API silently returns
 * zero results for `(language:A OR language:B)`-style OR-grouped qualifiers — the query is
 * accepted without error, it just never matches anything — so there is no working way to filter
 * by more than one language or topic at once server-side.
 */
data class DiscoverFilters(
    val language: String? = null,
    val topic: String? = null,
    val minStars: Int = 0,
    /** false = active repos only (default), true = archived repos only — GitHub's `archived:` qualifier. */
    val archived: Boolean = false,
)
