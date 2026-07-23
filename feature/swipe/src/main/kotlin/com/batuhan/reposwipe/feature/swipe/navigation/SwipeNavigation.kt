package com.batuhan.reposwipe.feature.swipe.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.swipe.SwipeScreen

const val SWIPE_ROUTE = "swipe"

fun NavGraphBuilder.swipeScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    composable(SWIPE_ROUTE) {
        SwipeScreen(onFiltersClick = onFiltersClick, onMenuClick = onMenuClick)
    }
}
