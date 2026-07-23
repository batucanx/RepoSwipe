package com.batuhan.reposwipe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.batuhan.reposwipe.R
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
import com.batuhan.reposwipe.feature.settings.navigation.SETTINGS_ROUTE
import com.batuhan.reposwipe.feature.settings.navigation.settingsScreen
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
fun RepoSwipeNavHost(
    startDestination: String,
    isAuthenticated: Boolean,
) {
    val navController = rememberNavController()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route
    val showBottomBar = currentRoute in MAIN_TAB_ROUTES

    // isAuthenticated flips to false if the token is revoked/expires mid-session (AuthInterceptor
    // clears it on a 401) or the user signs out from Profile — either way, drop them back to auth.
    var wasAuthenticated by remember { mutableStateOf(isAuthenticated) }
    LaunchedEffect(isAuthenticated) {
        if (wasAuthenticated && !isAuthenticated) {
            navController.navigate(AUTH_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
        wasAuthenticated = isAuthenticated
    }

    val discoverLabel = stringResource(R.string.nav_discover)
    val trendingLabel = stringResource(R.string.nav_trending)
    val starredLabel = stringResource(R.string.nav_starred)
    val profileLabel = stringResource(R.string.nav_profile)
    val bottomNavItems =
        remember(discoverLabel, trendingLabel, starredLabel, profileLabel) {
            listOf(
                RepoSwipeNavItem(SWIPE_ROUTE, discoverLabel, RepoSwipeIcons.Discover),
                RepoSwipeNavItem(LEADERBOARD_ROUTE, trendingLabel, RepoSwipeIcons.Trending),
                RepoSwipeNavItem(STARRED_ROUTE, starredLabel, RepoSwipeIcons.Star),
                RepoSwipeNavItem(PROFILE_ROUTE, profileLabel, RepoSwipeIcons.Profile),
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            authScreen(
                onAuthenticated = {
                    navController.navigate(SWIPE_ROUTE) {
                        popUpTo(AUTH_ROUTE) { inclusive = true }
                    }
                },
            )
            swipeScreen(
                onFiltersClick = { navController.navigate(FILTER_ROUTE) },
                onMenuClick = { navController.navigate(SETTINGS_ROUTE) },
            )
            leaderboardScreen(
                onFiltersClick = { navController.navigate(FILTER_ROUTE) },
                onMenuClick = { navController.navigate(SETTINGS_ROUTE) },
            )
            starredScreen(
                onFiltersClick = { navController.navigate(FILTER_ROUTE) },
                onMenuClick = { navController.navigate(SETTINGS_ROUTE) },
            )
            profileScreen(
                // Navigation on sign-out is handled reactively above via isAuthenticated,
                // uniformly with server-side session invalidation (401s).
                onSignedOut = {},
                onMenuClick = { navController.navigate(SETTINGS_ROUTE) },
            )
            filterScreen(onClose = { navController.popBackStack() })
            settingsScreen(onClose = { navController.popBackStack() })
        }
    }
}
