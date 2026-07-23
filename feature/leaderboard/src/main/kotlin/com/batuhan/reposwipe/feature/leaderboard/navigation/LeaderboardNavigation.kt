package com.batuhan.reposwipe.feature.leaderboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.leaderboard.LeaderboardScreen

const val LEADERBOARD_ROUTE = "leaderboard"

fun NavGraphBuilder.leaderboardScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    composable(LEADERBOARD_ROUTE) {
        LeaderboardScreen(onFiltersClick = onFiltersClick, onMenuClick = onMenuClick)
    }
}
