package com.batuhan.reposwipe.feature.followers.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.batuhan.reposwipe.feature.followers.PeopleScreen
import com.batuhan.reposwipe.feature.followers.PeopleTab

const val PEOPLE_INITIAL_TAB_ARG = "initialTab"
const val PEOPLE_ROUTE = "people/{$PEOPLE_INITIAL_TAB_ARG}"

/** Builds a concrete route so Profile's Followers/Following tiles land on the matching tab
 * instead of always opening to [PeopleTab.FOLLOWERS]. */
fun peopleRoute(initialTab: PeopleTab): String = "people/${initialTab.name}"

fun NavGraphBuilder.peopleScreen(onClose: () -> Unit) {
    composable(
        route = PEOPLE_ROUTE,
        arguments = listOf(navArgument(PEOPLE_INITIAL_TAB_ARG) { type = NavType.StringType }),
    ) {
        PeopleScreen(onClose = onClose)
    }
}
