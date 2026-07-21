package com.batuhan.reposwipe.feature.filter.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.filter.FilterScreen

const val FILTER_ROUTE = "filter"

fun NavGraphBuilder.filterScreen(onClose: () -> Unit) {
    composable(FILTER_ROUTE) {
        FilterScreen(onClose = onClose)
    }
}
