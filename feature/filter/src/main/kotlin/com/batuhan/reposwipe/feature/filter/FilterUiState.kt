package com.batuhan.reposwipe.feature.filter

import com.batuhan.reposwipe.core.data.model.DiscoverFilters

const val MAX_STARS = 50_000
const val MIN_STARS_FLOOR = 0
const val STAR_STEP = 500

data class FilterUiState(
    val filters: DiscoverFilters = DiscoverFilters(),
)
