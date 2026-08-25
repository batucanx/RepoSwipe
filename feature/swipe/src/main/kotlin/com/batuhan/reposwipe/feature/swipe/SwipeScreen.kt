package com.batuhan.reposwipe.feature.swipe

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.common.format.toClockTimeLabel
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.common.format.toRelativeTimeLabel
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.common.share.shareRepoIntent
import com.batuhan.reposwipe.core.data.model.Contributor
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoCard
import com.batuhan.reposwipe.core.designsystem.component.RepoCardData
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButton
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButtonSize
import com.batuhan.reposwipe.core.designsystem.component.SwipeDeck
import com.batuhan.reposwipe.core.designsystem.component.SwipeDeckState
import com.batuhan.reposwipe.core.designsystem.component.rememberSwipeDeckState
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import com.batuhan.reposwipe.feature.swipe.component.ReadmeWebView
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val readmeState by viewModel.readmeState.collectAsStateWithLifecycle()
    val languageBreakdownState by viewModel.languageBreakdownState.collectAsStateWithLifecycle()
    val contributorsState by viewModel.contributorsState.collectAsStateWithLifecycle()
    val similarReposState by viewModel.similarReposState.collectAsStateWithLifecycle()
    val detailStarred by viewModel.detailStarred.collectAsStateWithLifecycle()
    var repoForDetail by remember { mutableStateOf<Repo?>(null) }
    val deckState = rememberSwipeDeckState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(
            onMenuClick = onMenuClick,
            onTrailingClick = onFiltersClick,
            secondaryTrailingIcon = RepoSwipeIcons.Search,
            secondaryTrailingContentDescription = stringResource(R.string.swipe_search_cd),
            onSecondaryTrailingClick = onSearchClick,
        )

        LazyRow(
            contentPadding =
                PaddingValues(
                    horizontal = RepoSwipeTheme.spacing.gutter,
                    vertical = RepoSwipeTheme.spacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
        ) {
            items(DiscoverLanguages) { language ->
                RepoSwipeFilterChip(
                    label = language,
                    selected = language == filters.language,
                    onClick = { viewModel.selectLanguage(language) },
                )
            }
        }

        val currentRateLimit = rateLimit
        if (currentRateLimit != null && currentRateLimit.isExhausted) {
            FullScreenState {
                EmptyState(
                    icon = RepoSwipeIcons.RateLimited,
                    title = stringResource(R.string.swipe_rate_limit_title),
                    message =
                        stringResource(
                            R.string.swipe_rate_limit_message,
                            currentRateLimit.resetEpochSeconds.toClockTimeLabel(),
                        ),
                )
            }
        } else {
            // Two async signals both have to settle before the current snapshot can be trusted:
            // - mediator.refresh: RepoRemoteMediator's network fetch + Room write for the new
            //   filter. Source.refresh alone can go NotLoading the instant a (stale, previous-
            //   filter) row is read back, before this even starts — checking mediator too is what
            //   stops a leftover repo from the last language selection flashing on screen.
            // - source.refresh: the *local* Room query Paging re-runs once Room's invalidation
            //   tracker notices RepoRemoteMediator's write. That notification lands a beat after
            //   the mediator's DB transaction commits, so mediator.refresh alone can already read
            //   NotLoading while itemCount still reflects the pre-write (often 0, mid-filter-
            //   change) snapshot — flashing "No results found" for a frame before the freshly
            //   inserted rows are actually visible. Requiring neither to be Loading closes that
            //   gap without reopening the mediator-vs-source race described above.
            //
            // hasShownContent guards the *same* spinner against a second kind of churn — see
            // shouldShowFullScreenLoading's doc.
            var hasShownContent by remember(filters) { mutableStateOf(false) }
            val mediatorRefresh = repos.loadState.mediator?.refresh
            val sourceRefresh = repos.loadState.source.refresh
            when {
                shouldShowFullScreenLoading(mediatorRefresh, sourceRefresh, hasShownContent) ->
                    FullScreenState { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                mediatorRefresh is LoadState.Error || sourceRefresh is LoadState.Error ->
                    FullScreenState {
                        EmptyState(
                            icon = RepoSwipeIcons.Error,
                            title = stringResource(R.string.swipe_repos_error_title),
                            message = stringResource(R.string.swipe_repos_error_message),
                            iconTint = MaterialTheme.colorScheme.error,
                            actionLabel = stringResource(R.string.swipe_action_retry),
                            onAction = repos::retry,
                        )
                    }
                else -> {
                    val visibleRepos = visibleRepos(repos, currentIndex)
                    if (visibleRepos.isNotEmpty()) hasShownContent = true

                    if (visibleRepos.isEmpty()) {
                        FullScreenState {
                            if (repos.loadState.append is LoadState.Loading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            } else if (repos.itemCount == 0) {
                                EmptyState(
                                    icon = RepoSwipeIcons.NoResults,
                                    title = stringResource(R.string.swipe_no_results_title),
                                    message = stringResource(R.string.swipe_no_results_message),
                                )
                            } else {
                                EmptyState(
                                    icon = RepoSwipeIcons.Apply,
                                    title = stringResource(R.string.swipe_all_seen_title),
                                    message = stringResource(R.string.swipe_all_seen_message),
                                )
                            }
                        }
                    } else {
                        SwipeDeckContent(
                            repos = visibleRepos,
                            deckState = deckState,
                            onSwiped = { repo, direction -> viewModel.onSwiped(repo, direction) },
                            onRewind = viewModel::onRewind,
                            onQuickView = { repoForDetail = it },
                        )
                    }
                }
            }
        }
    }

    repoForDetail?.let { repo ->
        RepoDetailSheet(
            repo = repo,
            content = RepoDetailContentState(readmeState, languageBreakdownState, contributorsState, similarReposState),
            isStarredOnGitHub = detailStarred,
            onOpened = viewModel::onDetailOpened,
            onToggleGitHubStar = viewModel::toggleDetailStar,
            onDismiss = {
                repoForDetail = null
                viewModel.onDetailClosed()
            },
            onRewind = viewModel::onRewind,
            onReject = { coroutineScope.launch { deckState.swipeLeft() } },
            onStar = { coroutineScope.launch { deckState.swipeRight() } },
        )
    }
}

@Composable
private fun SwipeDeckContent(
    repos: List<Repo>,
    deckState: SwipeDeckState,
    onSwiped: (Repo, SwipeDirection) -> Unit,
    onRewind: () -> Unit,
    onQuickView: (Repo) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val frontRepo = repos.firstOrNull()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RepoSwipeTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SwipeDeck(
            items = repos,
            itemKey = { it.id },
            onSwiped = onSwiped,
            state = deckState,
            leftActionLabel = stringResource(R.string.swipe_action_pass),
            rightActionLabel = stringResource(R.string.swipe_action_star),
            onCardTap = onQuickView,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f),
        ) { repo ->
            // remember, not a bare call: this lambda is re-invoked whenever the deck recomposes,
            // and toCardData does real per-call work (two toCompactCount formats plus a
            // languageColor lookup) to rebuild a value that only changes when `repo` does.
            val cardData = remember(repo) { repo.toCardData() }
            RepoCard(data = cardData, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = RepoSwipeTheme.spacing.xl),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = RepoSwipeIcons.Rewind,
                contentDescription = stringResource(R.string.swipe_action_rewind_cd),
                onClick = onRewind,
                size = SwipeActionButtonSize.Large,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Skip,
                contentDescription = stringResource(R.string.swipe_action_pass),
                onClick = { coroutineScope.launch { deckState.swipeLeft() } },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Like,
                contentDescription = stringResource(R.string.swipe_action_star),
                onClick = { coroutineScope.launch { deckState.swipeRight() } },
                size = SwipeActionButtonSize.ExtraLarge,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                borderColor = MaterialTheme.colorScheme.primaryContainer,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.QuickView,
                contentDescription = stringResource(R.string.swipe_action_quick_view_cd),
                onClick = { frontRepo?.let(onQuickView) },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primaryContainer,
                borderColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun FullScreenState(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

/**
 * Whether the Discover deck's full-screen spinner should cover the screen right now.
 *
 * Every REFRESH [com.batuhan.reposwipe.core.data.RepoRemoteMediator] writes (clearForQuery +
 * insertAll, one transaction) invalidates Room's PagingSource and spins up a brand-new generation,
 * whose own local reload flips [sourceRefresh] back to [LoadState.Loading] for a beat even though
 * nothing the user cares about changed. Left unguarded, that alone was enough to snap the
 * full-screen spinner back over an already-visible deck 2-3 times in a row on cold start, reading
 * as the screen "loading" repeatedly. [hasShownContent] latches once real cards have rendered for
 * the current filter set (the call site's `remember(filters)` key resets it on a real filter
 * change, where re-showing the spinner is exactly what avoids the stale-card flash described in
 * [SwipeScreen]'s own loading-state comment) so that once-settled churn no longer punches the
 * spinner back over content that's already on screen.
 */
@VisibleForTesting
internal fun shouldShowFullScreenLoading(
    mediatorRefresh: LoadState?,
    sourceRefresh: LoadState,
    hasShownContent: Boolean,
): Boolean = !hasShownContent && (mediatorRefresh is LoadState.Loading || sourceRefresh is LoadState.Loading)

/** The (up to) 3 repos the deck currently shows/peeks, starting at [currentIndex] into [repos]'s
 * already-loaded pages. */
private fun visibleRepos(
    repos: LazyPagingItems<Repo>,
    currentIndex: Int,
): List<Repo> = (currentIndex until minOf(currentIndex + 3, repos.itemCount)).mapNotNull { index -> repos[index] }

/** Bundles the detail sheet's three independently-loading async sections into one parameter,
 * keeping [RepoDetailSheet]'s own parameter list from growing every time another section is added. */
private data class RepoDetailContentState(
    val readme: ReadmeUiState,
    val languageBreakdown: LanguageBreakdownUiState,
    val contributors: ContributorsUiState,
    val similarRepos: SimilarReposUiState,
)

private fun Repo.toCardData(): RepoCardData =
    RepoCardData(
        ownerAvatarUrl = ownerAvatarUrl,
        ownerLogin = ownerLogin,
        name = name,
        description = description,
        headerImageUrl = headerImageUrl,
        starCount = starCount.toCompactCount(),
        forkCount = forkCount.toCompactCount(),
        languageName = language,
        languageColor = languageColor(language),
    )

/**
 * Repo detail sheet, opened by tapping the front card or its quick-view button. The body scrolls
 * independently while [RepoDetailActionBar] stays pinned at the bottom, so the same swipe/rewind
 * decision can be made at any scroll position. Reject/star/rewind hide the sheet and drive the
 * shared deck state at the same time, so the card's own commit animation and the sheet's
 * slide-down play together. Everything in this sheet stays in-app except the small icon next to
 * the title and the README itself — [ReadmeWebView] renders GitHub's own README HTML and hands
 * any link/badge tap off to Chrome Custom Tabs, since that content and its links are GitHub's,
 * not this app's, the same as tapping one would do on github.com itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoDetailSheet(
    repo: Repo,
    content: RepoDetailContentState,
    isStarredOnGitHub: Boolean?,
    onOpened: (Repo) -> Unit,
    onToggleGitHubStar: () -> Unit,
    onDismiss: () -> Unit,
    onRewind: () -> Unit,
    onReject: () -> Unit,
    onStar: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(repo.id) { onOpened(repo) }

    // Hoisted out of the LazyColumn below on purpose. `remember` inside a lazy item dies when that
    // item scrolls out of the viewport and gets disposed, and the README is the *last* item — so
    // reading it, scrolling up to the contributors, then coming back would silently collapse it
    // again and re-run its "Devamını gör" affordance from scratch. Keeping both here means the
    // reader's decision to expand outlives any amount of scrolling, and the measured height is
    // still known if the WebView itself has to be re-inflated, so the button doesn't flicker back
    // in while a fresh measurement lands. Keyed to the sheet, which is per-repo.
    var readmeExpanded by remember { mutableStateOf(false) }
    var readmeContentHeightPx by remember { mutableIntStateOf(0) }

    fun hideThenDismiss() {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // LazyColumn rather than a plain scrolling Column: the sections below the fold
            // (contributors/similar-repos avatar rows, and especially the README's WebView —
            // real Android View inflation, one of the more expensive things this sheet does)
            // otherwise composed unconditionally the instant the sheet opened, competing with its
            // own slide-up animation for the frame budget. Composing lazily defers that cost until
            // the user actually scrolls each section into view.
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding =
                    PaddingValues(
                        start = RepoSwipeTheme.spacing.gutter,
                        end = RepoSwipeTheme.spacing.gutter,
                        bottom = RepoSwipeTheme.spacing.lg,
                    ),
                verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
            ) {
                item {
                    RepoDetailHeader(
                        repo = repo,
                        isStarredOnGitHub = isStarredOnGitHub,
                        onToggleGitHubStar = onToggleGitHubStar,
                    )
                }

                val language = repo.language
                if (language != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .background(languageColor(language) ?: MaterialTheme.colorScheme.outline, CircleShape),
                            )
                            Text(
                                text = language,
                                style = RepoSwipeTheme.typography.labelMd,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
                    ) {
                        DetailStatTile(
                            icon = RepoSwipeIcons.Star,
                            value = repo.starCount.toCompactCount(),
                            label = stringResource(R.string.swipe_detail_stat_stars),
                            modifier = Modifier.weight(1f),
                        )
                        DetailStatTile(
                            icon = RepoSwipeIcons.Fork,
                            value = repo.forkCount.toCompactCount(),
                            label = stringResource(R.string.swipe_detail_stat_forks),
                            modifier = Modifier.weight(1f),
                        )
                        DetailStatTile(
                            icon = RepoSwipeIcons.UpdatedAt,
                            value = repo.updatedAt.toRelativeTimeLabel(),
                            label = stringResource(R.string.swipe_detail_stat_updated),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item { LanguageBreakdownSection(state = content.languageBreakdown) }
                item { ContributorsSection(state = content.contributors) }
                item { SimilarReposSection(state = content.similarRepos) }
                item {
                    ReadmeSection(
                        state = content.readme,
                        expanded = readmeExpanded,
                        onExpand = { readmeExpanded = true },
                        contentHeightPx = readmeContentHeightPx,
                        onContentHeightMeasured = { readmeContentHeightPx = it },
                    )
                }
            }

            RepoDetailActionBar(
                onRewind = {
                    onRewind()
                    hideThenDismiss()
                },
                onReject = {
                    onReject()
                    hideThenDismiss()
                },
                onStar = {
                    onStar()
                    hideThenDismiss()
                },
            )
        }
    }
}

/** Repo name/description plus the sheet's three trailing actions: the real GitHub star toggle,
 * share, and the one link out to github.com. */
@Composable
private fun RepoDetailHeader(
    repo: Repo,
    isStarredOnGitHub: Boolean?,
    onToggleGitHubStar: () -> Unit,
) {
    val context = LocalContext.current
    val isStarred = isStarredOnGitHub == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${repo.ownerLogin}/${repo.name}",
                // Same reasoning as LeaderboardRow: headlineMd(28sp) suits the hero swipe card,
                // not a secondary detail sheet — matches RepoListItem's list-row name size.
                style = RepoSwipeTheme.typography.bodyLg.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = repo.description,
                style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            // The real GitHub star, separate from the deck's swipe-right: this one reflects the
            // repo's actual starred state and leaves the sheet open.
            IconButton(onClick = onToggleGitHubStar) {
                Icon(
                    imageVector = if (isStarred) RepoSwipeIcons.StarFilled else RepoSwipeIcons.Star,
                    contentDescription =
                        if (isStarred) {
                            stringResource(R.string.swipe_detail_unstar_cd)
                        } else {
                            stringResource(R.string.swipe_detail_star_cd)
                        },
                    tint =
                        if (isStarred) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            IconButton(onClick = { context.startActivity(shareRepoIntent(repo.htmlUrl)) }) {
                Icon(
                    imageVector = RepoSwipeIcons.Share,
                    contentDescription = stringResource(R.string.swipe_detail_share_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))) },
            ) {
                Icon(
                    imageVector = RepoSwipeIcons.OpenExternal,
                    contentDescription = stringResource(R.string.swipe_action_open_github_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .padding(vertical = RepoSwipeTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = value,
            style = RepoSwipeTheme.typography.headlineMd.copy(fontSize = 16.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Proportional stacked bar + legend, built from GitHub's per-language byte counts. Silently
 * shows nothing while loading/on error/for a repo with no detectable languages — this is
 * supplementary detail, not something worth blocking or erroring the whole sheet over. */
@Composable
private fun LanguageBreakdownSection(
    state: LanguageBreakdownUiState,
    modifier: Modifier = Modifier,
) {
    if (state !is LanguageBreakdownUiState.Loaded) return
    val total = state.languages.values.sum()
    if (total <= 0L) return
    // Capped so a monorepo with dozens of detected languages doesn't turn this into a wall of
    // text — the bar itself still reflects every language's true share, only the legend is capped.
    val sorted = state.languages.entries.sortedByDescending { it.value }
    val legend = sorted.take(MAX_LANGUAGE_LEGEND_ENTRIES)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.swipe_detail_languages_title),
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(LANGUAGE_BAR_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(percent = 50)),
        ) {
            sorted.forEach { (name, bytes) ->
                Box(
                    modifier =
                        Modifier
                            .weight((bytes.toFloat() / total).coerceAtLeast(MIN_LANGUAGE_SEGMENT_WEIGHT))
                            .fillMaxHeight()
                            .background(languageColor(name) ?: MaterialTheme.colorScheme.outline),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base)) {
            legend.forEach { (name, bytes) ->
                val percent = (bytes.toFloat() / total * PERCENT_SCALE).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .background(languageColor(name) ?: MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Text(
                        text = stringResource(R.string.swipe_detail_language_percent, name, percent),
                        style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Horizontally-scrollable avatar row, tap-through to each contributor's GitHub profile. Shows
 * nothing while loading/on error/for a repo with no contributors data — same reasoning as
 * [LanguageBreakdownSection]. */
@Composable
private fun ContributorsSection(
    state: ContributorsUiState,
    modifier: Modifier = Modifier,
) {
    if (state !is ContributorsUiState.Loaded || state.contributors.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.swipe_detail_contributors_title),
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
            items(state.contributors, key = { it.login }) { contributor ->
                ContributorAvatar(contributor = contributor)
            }
        }
    }
}

@Composable
private fun ContributorAvatar(contributor: Contributor) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .width(CONTRIBUTOR_AVATAR_COLUMN_WIDTH_DP.dp)
                .clickable(enabled = contributor.htmlUrl != null) {
                    contributor.htmlUrl?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        AsyncImage(
            model = contributor.avatarUrl,
            contentDescription = contributor.login,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(CONTRIBUTOR_AVATAR_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), CircleShape),
        )
        Text(
            text = contributor.login,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontally-scrollable row of repos sharing this one's language/topic — see
 * [com.batuhan.reposwipe.core.data.RepoRepository.getSimilarRepos] for the heuristic. Each card
 * links straight out to github.com rather than opening a nested detail sheet. Shows nothing
 * while loading/on error/when the heuristic found nothing, same reasoning as the sections above. */
@Composable
private fun SimilarReposSection(
    state: SimilarReposUiState,
    modifier: Modifier = Modifier,
) {
    if (state !is SimilarReposUiState.Loaded || state.repos.isEmpty()) return
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.swipe_detail_similar_repos_title),
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
            items(state.repos, key = { it.id }) { repo ->
                SimilarRepoCard(
                    repo = repo,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))) },
                )
            }
        }
    }
}

@Composable
private fun SimilarRepoCard(
    repo: Repo,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(SIMILAR_REPO_CARD_WIDTH_DP.dp)
                // Clips clickable()'s ripple to the rounded shape below — without it the ripple
                // draws across the full rectangular bounds instead of the rounded card.
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), MaterialTheme.shapes.large)
                .clickable(onClick = onClick)
                .padding(RepoSwipeTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        Text(
            text = "${repo.ownerLogin}/${repo.name}",
            style = RepoSwipeTheme.typography.bodySm.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
        ) {
            Icon(
                imageVector = RepoSwipeIcons.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = repo.starCount.toCompactCount(),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// How much README the detail sheet shows before "Devamını gör". Sized in dp rather than in HTML
// characters (which is what an earlier truncate-the-markup approach used) because what actually
// matters is how much vertical space the README is allowed to take from the rest of the sheet —
// a character budget gave wildly different visual heights depending on how image-heavy the README
// was. Roughly a phone screen's worth: enough to judge the repo, short enough that the sections
// below the README stay reachable without a long scroll.
private const val README_COLLAPSED_HEIGHT_DP = 420
private const val MAX_LANGUAGE_LEGEND_ENTRIES = 6
private const val MIN_LANGUAGE_SEGMENT_WEIGHT = 0.001f
private const val LANGUAGE_BAR_HEIGHT_DP = 8
private const val PERCENT_SCALE = 100
private const val CONTRIBUTOR_AVATAR_SIZE_DP = 48
private const val CONTRIBUTOR_AVATAR_COLUMN_WIDTH_DP = 56
private const val SIMILAR_REPO_CARD_WIDTH_DP = 160

/** Expand state is owned by [RepoDetailSheet] rather than by this composable — see the declaration
 * of `readmeExpanded` there for why surviving lazy-item disposal matters. */
@Composable
private fun ReadmeSection(
    state: ReadmeUiState,
    expanded: Boolean,
    onExpand: () -> Unit,
    contentHeightPx: Int,
    onContentHeightMeasured: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .padding(RepoSwipeTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
        ) {
            Icon(
                imageVector = RepoSwipeIcons.RepoPlaceholder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.swipe_detail_readme_title),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when (state) {
            is ReadmeUiState.Loading ->
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = RepoSwipeTheme.spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            is ReadmeUiState.Error ->
                Text(
                    text = stringResource(R.string.swipe_detail_readme_error),
                    style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            is ReadmeUiState.Loaded -> {
                val html = state.html
                if (html.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.swipe_detail_readme_empty),
                        style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Expansion is deliberately one-way: there is no "daha az göster". Collapsing
                    // shrinks this item inside the sheet's LazyColumn, which clamps the list's
                    // scroll offset and throws the reader somewhere they didn't ask to be — the
                    // exact disorientation the expand path was fixed to avoid. Once someone has
                    // asked for the whole README, keeping it open costs them nothing: the sheet is
                    // already scrollable, and dismissing it resets the state anyway.
                    val collapsedHeightPx =
                        with(LocalDensity.current) { README_COLLAPSED_HEIGHT_DP.dp.roundToPx() }

                    ReadmeWebView(
                        html = html,
                        modifier = Modifier.fillMaxWidth(),
                        // null lifts the cap entirely, revealing the already-rendered full document
                        // — no reload, no re-parse, nothing to wait for. See ReadmeWebView's doc.
                        maxVisibleHeightPx = if (expanded) null else collapsedHeightPx,
                        initialContentHeightPx = contentHeightPx,
                        onContentHeightMeasured = onContentHeightMeasured,
                    )

                    // Only worth offering once a real measurement has landed and it actually
                    // exceeds the collapsed window — a short README is fully visible already.
                    if (!expanded && contentHeightPx > collapsedHeightPx) {
                        TextButton(onClick = onExpand) {
                            Text(
                                text = stringResource(R.string.swipe_detail_readme_show_more),
                                style = RepoSwipeTheme.typography.labelMd,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Sticky footer inside [RepoDetailSheet] — same buttons/styling as the Discover action row
 * (minus quick-view, since you're already looking at the detail). No GitHub/external-nav button
 * here on purpose: the small icon next to the title is the only way out of the app from this sheet. */
@Composable
private fun RepoDetailActionBar(
    onRewind: () -> Unit,
    onReject: () -> Unit,
    onStar: () -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = RepoSwipeTheme.spacing.gutter, vertical = RepoSwipeTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = RepoSwipeIcons.Rewind,
                contentDescription = stringResource(R.string.swipe_action_rewind_cd),
                onClick = onRewind,
                size = SwipeActionButtonSize.ExtraSmall,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Skip,
                contentDescription = stringResource(R.string.swipe_action_pass),
                onClick = onReject,
                size = SwipeActionButtonSize.ExtraSmall,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Like,
                contentDescription = stringResource(R.string.swipe_action_star),
                onClick = onStar,
                size = SwipeActionButtonSize.Small,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                borderColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}
