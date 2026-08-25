package com.batuhan.reposwipe.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.auth.AuthScreen

const val AUTH_ROUTE = "auth"

fun NavGraphBuilder.authScreen(onAuthenticated: () -> Unit) {
    composable(AUTH_ROUTE) {
        AuthScreen(onAuthenticated = onAuthenticated)
    }
}
