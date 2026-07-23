package com.batuhan.reposwipe.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.profile.ProfileScreen

const val PROFILE_ROUTE = "profile"

fun NavGraphBuilder.profileScreen(
    onSignedOut: () -> Unit,
    onMenuClick: () -> Unit,
) {
    composable(PROFILE_ROUTE) {
        ProfileScreen(onSignedOut = onSignedOut, onMenuClick = onMenuClick)
    }
}
