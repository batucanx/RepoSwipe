package com.batuhan.reposwipe.feature.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.data.model.LeaderboardEntry
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.collectIsScrollingDownAsState
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.CardBackgroundNavy
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor

// Podium tiers — rank 1 keeps a signature two-tone gradient; 2nd/3rd get silver/bronze so the top
// of the list reads as a hierarchy at a glance instead of a flat list. The gradient's blue→purple
// pairing (from the brand accent's old "Matte Neutral + Violet" purple) is now blue→navy
// (2026-09-04, French-tricolor reskin exploration) since that purple is gone from the palette —
// the blue start stop is untouched, still the app's general accent color.
private val GoldGradient = Brush.linearGradient(listOf(Color(0xFF0969DA), CardBackgroundNavy))
private val SilverGradient = Brush.linearGradient(listOf(Color(0xFF9AA5B5), Color(0xFF6B7688)))
private val BronzeGradient = Brush.linearGradient(listOf(Color(0xFFC97C4B), Color(0xFF8B4F2A)))

// Frosted-glass row styling: a faint onSurface wash plus a barely-there rim of the same color,
// tuned so the row reads as a translucent panel rather than either a flat gray card or a
// barely-visible line.
private const val GLASS_FILL_ALPHA = 0.06f
private const val GLASS_BORDER_ALPHA = 0.12f

private fun podiumGradient(rank: Int): Brush? =
    when (rank) {
        1 -> GoldGradient
        2 -> SilverGradient
        3 -> BronzeGradient
        else -> null
    }

@Composable
fun LeaderboardScreen(
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onOpenDetail: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val starredStates by viewModel.starredStates.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isScrollingDown by listState.collectIsScrollingDownAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // The whole bar collapses away (not just its logo) so the list below actually gains that
        // screen space while scrolled, instead of the bar staying the same height with an empty
        // gap where the logo used to be.
        AnimatedVisibility(
            visible = !isScrollingDown || listState.firstVisibleItemIndex == 0,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            RepoSwipeTopAppBar(
                onMenuClick = onMenuClick,
                onTrailingClick = onSearchClick,
                trailingIcon = RepoSwipeIcons.Search,
                trailingContentDescription = stringResource(R.string.leaderboard_search_cd),
            )
        }

        when {
            uiState.isLoading ->
                FullScreenState {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            uiState.error != null ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Error,
                        title = stringResource(R.string.leaderboard_error_title),
                        message = uiState.error?.asString().orEmpty(),
                        iconTint = MaterialTheme.colorScheme.error,
                        actionLabel = stringResource(R.string.leaderboard_action_retry),
                        onAction = viewModel::retry,
                    )
                }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(RepoSwipeTheme.spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                    ) {
                        item { LeaderboardHeader() }

                        if (uiState.entries.isEmpty()) {
                            item {
                                // Centered within the visible viewport rather than pinned right
                                // under the header — at its natural height inside a LazyColumn it
                                // left a large dead gap below instead of reading as balanced.
                                Box(
                                    modifier = Modifier.fillMaxWidth().fillParentMaxHeight(0.7f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    EmptyState(
                                        icon = RepoSwipeIcons.Leaderboard,
                                        title = stringResource(R.string.leaderboard_empty_title),
                                        message = stringResource(R.string.leaderboard_empty_message),
                                        actionLabel = stringResource(R.string.leaderboard_empty_action),
                                        onAction = onNavigateToDiscover,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(uiState.entries, key = { _, entry -> entry.repoId }) { index, entry ->
                                LeaderboardRow(
                                    rank = index + 1,
                                    entry = entry,
                                    starred = starredStates[starKey(entry)] == true,
                                    onOpenDetail = { onOpenDetail(entry.ownerLogin, entry.repoName) },
                                    onToggleStar = { viewModel.toggleStar(entry) },
                                )
                            }

                            if (uiState.hasMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Button(
                                                onClick = viewModel::loadMore,
                                                shape = MaterialTheme.shapes.extraLarge,
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                    ),
                                            ) {
                                                Text(text = stringResource(R.string.leaderboard_load_more))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenState(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun LeaderboardHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
        ) {
            Icon(
                imageVector = RepoSwipeIcons.Trending,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.leaderboard_global_rankings),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.leaderboard_title),
            // displayLg(48sp) is sized for a one-off hero moment, not a header that scrolls
            // alongside a dense list every time — headlineLgMobile matches how Profile/Filters
            // size their own screen titles.
            style = RepoSwipeTheme.typography.headlineLgMobile,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.leaderboard_subtitle),
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    starred: Boolean,
    onOpenDetail: () -> Unit,
    onToggleStar: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val isChampion = rank == 1
    // onSurface rather than a fixed color so the glass rim/wash lightens against dark theme's
    // near-black surface and darkens against light theme's near-white one — either direction
    // reads as "frosted," a single fixed tint wouldn't. Podium ranks keep their colored rim on
    // top of this; anything past 3rd gets the plain frosted edge instead of a flat outlineVariant
    // line, so it reads as a glass rim rather than a generic Material outline.
    val glassTint = MaterialTheme.colorScheme.onSurface
    val borderBrush = podiumGradient(rank) ?: SolidColor(glassTint.copy(alpha = GLASS_BORDER_ALPHA))
    // 0.22 read fine in dark mode (a near-black onSurface text sits on it regardless) but was too
    // opaque in light mode — primaryContainer there is near-black, so the tint muddied the card
    // and fought the onSurfaceVariant description text sitting on top of it. Softer alpha keeps
    // the champion card visually distinct without hurting either theme's readability.
    val championTintBrush =
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.14f),
            ),
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isChampion) 12.dp else 3.dp,
                    shape = shape,
                    ambientColor = if (isChampion) Color(0xFF0969DA).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f),
                    spotColor = if (isChampion) CardBackgroundNavy.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f),
                )
                // .background()/.border() are shape-aware for their own paint, but without this
                // clip the ripple clickable() draws below isn't — it renders across the row's
                // full rectangular layout bounds, poking a hard-edged rectangle out past the
                // rounded corners instead of respecting them.
                .clip(shape)
                // Opaque surfaceContainer first — it's what blocks the shadow above from bleeding
                // through the translucent glass tint that follows (see the fix this replaced: a
                // translucent fill directly over that shadow showed it through as a visible
                // rectangular smudge). The glass tint layers on top of that same opaque base in
                // both themes, rather than on the page background directly.
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .background(glassTint.copy(alpha = GLASS_FILL_ALPHA), shape)
                .then(if (isChampion) Modifier.background(championTintBrush, shape) else Modifier)
                .border(if (isChampion) 1.5.dp else 1.dp, borderBrush, shape)
                .clickable(onClick = onOpenDetail)
                .padding(RepoSwipeTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
    ) {
        RankBadge(rank)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${entry.ownerLogin}/${entry.repoName}",
                    // headlineMd(28sp) reads fine for a lone card title but is oversized repeated
                    // down a list — matches RepoListItem's own repo-name size on the Starred
                    // screen for the same "name in a list row" context.
                    style = RepoSwipeTheme.typography.bodyLg.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color =
                        if (isChampion) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                LeaderboardRowTrailing(
                    swipeCount = entry.swipeCount,
                    starred = starred,
                    onToggleStar = onToggleStar,
                )
            }

            Text(
                text = entry.description,
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val language = entry.language
            if (language != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .background(
                                    languageColor(language) ?: MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                    )
                    Text(
                        text = language,
                        style = RepoSwipeTheme.typography.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowTrailing(
    swipeCount: Long,
    starred: Boolean,
    onToggleStar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
    ) {
        Row(
            modifier =
                Modifier
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(percent = 50),
                    ).padding(horizontal = RepoSwipeTheme.spacing.sm, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
        ) {
            Icon(
                imageVector = RepoSwipeIcons.StarFilled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = swipeCount.toInt().toCompactCount(),
                style = RepoSwipeTheme.typography.statsNumber,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        // The real GitHub star, independent of the swipe deck — lets a repo be starred straight
        // from Leaderboard instead of only via the swipe/detail flows.
        IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (starred) RepoSwipeIcons.StarFilled else RepoSwipeIcons.Star,
                contentDescription =
                    if (starred) {
                        stringResource(R.string.leaderboard_unstar_cd)
                    } else {
                        stringResource(R.string.leaderboard_star_cd)
                    },
                tint =
                    if (starred) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val shape = MaterialTheme.shapes.medium
    val gradient = podiumGradient(rank)
    val badgeModifier =
        if (gradient != null) {
            Modifier
                .shadow(elevation = 6.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.3f))
                .background(gradient, shape)
        } else {
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        }
    Box(
        modifier = Modifier.size(40.dp).then(badgeModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = RepoSwipeTheme.typography.statsNumber,
            color = if (gradient != null) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}
