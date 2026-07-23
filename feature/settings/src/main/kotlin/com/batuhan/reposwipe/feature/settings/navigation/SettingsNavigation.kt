package com.batuhan.reposwipe.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.settings.SettingsScreen

const val SETTINGS_ROUTE = "settings"

fun NavGraphBuilder.settingsScreen(onClose: () -> Unit) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(onClose = onClose)
    }
}
