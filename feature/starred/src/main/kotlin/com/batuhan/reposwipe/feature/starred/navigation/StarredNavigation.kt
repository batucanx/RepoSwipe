package com.batuhan.reposwipe.feature.starred.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.starred.StarredScreen

const val STARRED_ROUTE = "starred"

fun NavGraphBuilder.starredScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    composable(STARRED_ROUTE) {
        StarredScreen(onFiltersClick = onFiltersClick, onMenuClick = onMenuClick)
    }
}
