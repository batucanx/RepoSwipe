package com.batuhan.reposwipe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeBottomNavBar
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeNavItem
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.feature.auth.navigation.AUTH_ROUTE
import com.batuhan.reposwipe.feature.auth.navigation.authScreen
import com.batuhan.reposwipe.feature.filter.navigation.FILTER_ROUTE
import com.batuhan.reposwipe.feature.filter.navigation.filterScreen
import com.batuhan.reposwipe.feature.leaderboard.navigation.LEADERBOARD_ROUTE
import com.batuhan.reposwipe.feature.leaderboard.navigation.leaderboardScreen
import com.batuhan.reposwipe.feature.profile.navigation.PROFILE_ROUTE
import com.batuhan.reposwipe.feature.profile.navigation.profileScreen
import com.batuhan.reposwipe.feature.starred.navigation.STARRED_ROUTE
import com.batuhan.reposwipe.feature.starred.navigation.starredScreen
import com.batuhan.reposwipe.feature.swipe.navigation.SWIPE_ROUTE
import com.batuhan.reposwipe.feature.swipe.navigation.swipeScreen

private val MAIN_TAB_ROUTES = setOf(SWIPE_ROUTE, LEADERBOARD_ROUTE, STARRED_ROUTE, PROFILE_ROUTE)

/**
 * Single NavHost wrapped in a Scaffold whose bottom bar only shows for the four main tabs —
 * hidden on [AUTH_ROUTE] (pre-login) and [FILTER_ROUTE] (full-screen modal), matching the
 * mockups' shell. See Now-in-Android's `NiaApp` for the same "one NavHost, conditional
 * bottom bar" pattern.
 */
@Composable
fun RepoSwipeNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in MAIN_TAB_ROUTES

    val bottomNavItems =
        remember {
            listOf(
                RepoSwipeNavItem(SWIPE_ROUTE, "Discover", RepoSwipeIcons.Discover),
                RepoSwipeNavItem(LEADERBOARD_ROUTE, "Trending", RepoSwipeIcons.Trending),
                RepoSwipeNavItem(STARRED_ROUTE, "Starred", RepoSwipeIcons.Star),
                RepoSwipeNavItem(PROFILE_ROUTE, "Profile", RepoSwipeIcons.Profile),
            )
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                RepoSwipeBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AUTH_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            authScreen(
                onAuthenticated = {
                    navController.navigate(SWIPE_ROUTE) {
                        popUpTo(AUTH_ROUTE) { inclusive = true }
                    }
                },
            )
            swipeScreen(onFiltersClick = { navController.navigate(FILTER_ROUTE) })
            leaderboardScreen(onFiltersClick = { navController.navigate(FILTER_ROUTE) })
            starredScreen(onFiltersClick = { navController.navigate(FILTER_ROUTE) })
            profileScreen(
                onSignedOut = {
                    navController.navigate(AUTH_ROUTE) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
            )
            filterScreen(onClose = { navController.popBackStack() })
        }
    }
}
