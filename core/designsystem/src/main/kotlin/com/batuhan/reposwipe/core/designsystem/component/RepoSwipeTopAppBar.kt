package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.modifier.bottomHairline
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun RepoSwipeTopAppBar(
    onMenuClick: () -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector = RepoSwipeIcons.Filters,
    trailingContentDescription: String = stringResource(R.string.topbar_filters_cd),
    // Optional second trailing action, e.g. Discover's search entry point — defaults to absent so
    // every existing call site (Leaderboard/Starred/...) renders exactly as before.
    secondaryTrailingIcon: ImageVector? = null,
    secondaryTrailingContentDescription: String? = null,
    onSecondaryTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .bottomHairline(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = RepoSwipeIcons.Menu,
                contentDescription = stringResource(R.string.topbar_menu_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        BrandWordmark(modifier = Modifier.height(BRAND_LOGO_HEIGHT_DP.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryTrailingIcon != null && onSecondaryTrailingClick != null) {
                IconButton(onClick = onSecondaryTrailingClick) {
                    Icon(
                        imageVector = secondaryTrailingIcon,
                        contentDescription = secondaryTrailingContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onTrailingClick) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The "RepoSwipe" wordmark. Renders the illustrated logo lockup (white "Repo", the GitHub octocat
 * silhouette standing in for the "o", red "Swipe") rather than the app's own type/shimmer effect —
 * replacing 2026-09-04's animated shimmer-text treatment per explicit user request to use this logo
 * art everywhere the brand name previously appeared as plain text. Every screen that shows this
 * wordmark (this bar, Auth, Filter) shares this one composable rather than each duplicating its own
 * `Image(painterResource(...))` call, so a fix like the theme swap below only has to happen once.
 *
 * [R.drawable.logo_reposwipe_wordmark] pairs white "Repo" text with a transparent background sized
 * for a dark surface, and is illegible on light theme's near-white one, so light theme swaps in
 * [R.drawable.logo_reposwipe_wordmark_light] (navy "Repo" text, same red "Swipe"/octocat, also
 * transparent) instead of reusing the dark asset there. Both assets are cropped to the same
 * ink-to-canvas ratio so they render at matching apparent size at any shared `Modifier.height(...)`
 * — a raw, uncropped export from either theme would otherwise look smaller than the other purely
 * from carrying more baked-in padding. Callers size this via `Modifier.height(...)`, matching this
 * composable's own [ContentScale.FillHeight].
 */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.topbar_title_default),
) {
    val logo = if (RepoSwipeTheme.isDarkTheme) R.drawable.logo_reposwipe_wordmark else R.drawable.logo_reposwipe_wordmark_light
    Image(
        painter = painterResource(id = logo),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.FillHeight,
    )
}

private const val BRAND_LOGO_HEIGHT_DP = 48

/**
 * Tracks scroll *direction* (not just position) off a [LazyListState] — true while the user's most
 * recent scroll delta moved content up (i.e. scrolling down the list), false while it moved
 * content down (scrolling back up). Meant to drive whether a caller shows [RepoSwipeTopAppBar] at
 * all: wrap the whole call in `AnimatedVisibility(visible = !isScrollingDown ||
 * listState.firstVisibleItemIndex == 0, exit = shrinkVertically() + fadeOut(), ...)` so the bar's
 * full height collapses away (handing that space to the list below) rather than just hiding its
 * logo inside a bar that stays the same size — see Leaderboard/StarredScreen for the call site.
 * Pairing with `firstVisibleItemIndex == 0` brings the bar back the instant the list is back at
 * the top, rather than only on an explicit upward scroll (which a fast fling back to the top, or
 * `PullToRefreshBox`'s own bounce, might not register as one).
 */
@Composable
fun LazyListState.collectIsScrollingDownAsState(): State<Boolean> {
    val isScrollingDown = remember(this) { mutableStateOf(false) }
    // A LaunchedEffect + snapshotFlow collector, not derivedStateOf: the previous-index/offset
    // bookkeeping below has to *mutate* state as each new value arrives, and derivedStateOf's
    // calculation is meant to be a pure read — mutating state it also reads from inside that
    // calculation confused its own invalidation tracking, so the reported direction got stuck
    // instead of updating on every scroll delta (the bar never collapsed once scrolled).
    LaunchedEffect(this) {
        var previousIndex = firstVisibleItemIndex
        var previousScrollOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                isScrollingDown.value =
                    if (previousIndex != index) {
                        previousIndex < index
                    } else {
                        previousScrollOffset < offset
                    }
                previousIndex = index
                previousScrollOffset = offset
            }
    }
    return isScrollingDown
}
