package com.batuhan.reposwipe.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.auth.onboarding.OnboardingScreen

const val ONBOARDING_ROUTE = "onboarding"

fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable(ONBOARDING_ROUTE) {
        OnboardingScreen(onFinished = onFinished)
    }
}
