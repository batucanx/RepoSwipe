package com.batuhan.reposwipe.feature.filter.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.batuhan.reposwipe.feature.filter.SearchScreen

const val SEARCH_ROUTE = "search"

fun NavGraphBuilder.searchScreen(onClose: () -> Unit) {
    composable(SEARCH_ROUTE) {
        SearchScreen(onClose = onClose)
    }
}
