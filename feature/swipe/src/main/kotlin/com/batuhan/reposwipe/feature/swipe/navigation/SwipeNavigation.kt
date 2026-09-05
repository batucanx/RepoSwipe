package com.batuhan.reposwipe.feature.swipe.navigation

import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.batuhan.reposwipe.feature.swipe.DetailDeckAction
import com.batuhan.reposwipe.feature.swipe.RepoDetailScreen
import com.batuhan.reposwipe.feature.swipe.SwipeScreen

const val SWIPE_ROUTE = "swipe"
const val REPO_DETAIL_ROUTE = "repo_detail"
const val REPO_OWNER_ARG = "owner"
const val REPO_NAME_ARG = "repo"
const val DETAIL_ACTION_RESULT = "detail_action_result"
private const val FROM_DECK_ARG = "fromDeck"
private const val REPO_DETAIL_ROUTE_PATTERN =
    "$REPO_DETAIL_ROUTE/{$REPO_OWNER_ARG}/{$REPO_NAME_ARG}?$FROM_DECK_ARG={$FROM_DECK_ARG}"

/**
 * [fromDeck] gates [RepoDetailScreen]'s bottom Rewind/Pass/Star bar, which drives the Discover
 * swipe deck (and only makes sense there) — entry points with no deck behind them (Leaderboard,
 * Search) pass `false` so only the header's own real GitHub-star toggle is offered.
 */
fun repoDetailRoute(
    owner: String,
    repo: String,
    fromDeck: Boolean = true,
): String = "$REPO_DETAIL_ROUTE/${Uri.encode(owner)}/${Uri.encode(repo)}?$FROM_DECK_ARG=$fromDeck"

fun NavGraphBuilder.swipeScreen(
    onFiltersClick: () -> Unit,
    onOpenDetail: (owner: String, repo: String) -> Unit,
) {
    composable(SWIPE_ROUTE) { backStackEntry ->
        val pendingAction =
            backStackEntry.savedStateHandle
                .getStateFlow<String?>(DETAIL_ACTION_RESULT, null)
                .collectAsStateWithLifecycle()
                .value
                ?.let { value -> runCatching { DetailDeckAction.valueOf(value) }.getOrNull() }

        SwipeScreen(
            onFiltersClick = onFiltersClick,
            onOpenDetail = onOpenDetail,
            pendingDetailAction = pendingAction,
            onDetailActionConsumed = { backStackEntry.savedStateHandle[DETAIL_ACTION_RESULT] = null },
        )
    }
}

fun NavGraphBuilder.repoDetailScreen(
    onBack: () -> Unit,
    onDeckAction: (DetailDeckAction) -> Unit,
) {
    composable(
        route = REPO_DETAIL_ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(REPO_OWNER_ARG) { type = NavType.StringType },
                navArgument(REPO_NAME_ARG) { type = NavType.StringType },
                navArgument(FROM_DECK_ARG) {
                    type = NavType.BoolType
                    defaultValue = true
                },
            ),
    ) { backStackEntry ->
        val fromDeck = backStackEntry.arguments?.getBoolean(FROM_DECK_ARG) ?: true
        RepoDetailScreen(onBack = onBack, onDeckAction = onDeckAction, showDeckActions = fromDeck)
    }
}
