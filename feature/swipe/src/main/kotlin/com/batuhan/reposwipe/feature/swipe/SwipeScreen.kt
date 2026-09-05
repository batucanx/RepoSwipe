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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
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
import com.batuhan.reposwipe.core.data.model.AvailableLanguages
import com.batuhan.reposwipe.core.data.model.Contributor
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoCard
import com.batuhan.reposwipe.core.designsystem.component.RepoCardData
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButton
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButtonSize
import com.batuhan.reposwipe.core.designsystem.component.SwipeDeck
import com.batuhan.reposwipe.core.designsystem.component.SwipeDeckState
import com.batuhan.reposwipe.core.designsystem.component.rememberSwipeDeckState
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import com.batuhan.reposwipe.feature.swipe.component.README_PREVIEW_CAP_DP
import com.batuhan.reposwipe.feature.swipe.component.ReadmeViewMode
import com.batuhan.reposwipe.feature.swipe.component.ReadmeWebView
import com.batuhan.reposwipe.feature.swipe.component.ScrollGatedWebView
import com.batuhan.reposwipe.feature.swipe.component.rememberReadmeWebView
import kotlinx.coroutines.launch
import com.batuhan.reposwipe.core.designsystem.R as DesignSystemR

@Composable
fun SwipeScreen(
    onFiltersClick: () -> Unit,
    onOpenDetail: (owner: String, repo: String) -> Unit,
    pendingDetailAction: DetailDeckAction? = null,
    onDetailActionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    val deckState = rememberSwipeDeckState()
    val visibleRepos = visibleRepos(repos, currentIndex)

    // Hoisted above the loading/error/empty `when` below (rather than living inside
    // SwipeDeckContent, which only composes in its innermost success branch) so a Star/Pass/
    // Rewind action returned from the detail screen is never silently stranded — or, worse,
    // replayed against a different repo later — just because the deck wasn't composed at the
    // exact moment it came back (e.g. a filter-change reload was in progress). With no front
    // card to apply it to, it's simply consumed instead of misapplied.
    LaunchedEffect(pendingDetailAction) {
        val action = pendingDetailAction ?: return@LaunchedEffect
        if (visibleRepos.isNotEmpty()) {
            when (action) {
                DetailDeckAction.Rewind -> viewModel.onRewind()
                DetailDeckAction.Pass -> deckState.swipeLeft()
                DetailDeckAction.Star -> deckState.swipeRight()
            }
        }
        onDetailActionConsumed()
    }

    Box(modifier = modifier.fillMaxSize()) {
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
            // shouldShowFullScreenLoading's doc. Keying it on all filters keeps the quick-language
            // rail and the dedicated Filter screen on the same loading/state transition path.
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
                        // Warms the OkHttp disk cache for every repo the deck currently shows or
                        // peeks — see SwipeViewModel.prefetchReadme's doc. Keyed on the list itself
                        // (structural equality on Repo, a data class) so this re-fires exactly when
                        // the visible window actually shifts, i.e. once per swipe.
                        LaunchedEffect(visibleRepos) {
                            visibleRepos.forEach(viewModel::prefetchReadme)
                        }
                        SwipeDeckContent(
                            repos = visibleRepos,
                            deckState = deckState,
                            onSwiped = { repo, direction -> viewModel.onSwiped(repo, direction) },
                            onRewind = viewModel::onRewind,
                            onQuickView = { repo -> onOpenDetail(repo.ownerLogin, repo.name) },
                            // Small top clearance so the card's header image doesn't sit flush
                            // against the top edge — on Samsung devices with a center-top camera
                            // cutout, the full-bleed header otherwise reads as touching it.
                            modifier =
                                Modifier.padding(
                                    top = RepoSwipeTheme.spacing.xs,
                                    bottom = RepoSwipeTheme.spacing.sm,
                                ),
                        )
                    }
                }
            }
        }

        SwipeTopControls(
            onFiltersClick = onFiltersClick,
            selectedLanguage = filters.language,
            onLanguageSelected = viewModel::setQuickLanguage,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Discover's fixed filter affordance plus a single-select, horizontally scrolling language rail.
 * It floats over the card's top scrim so the deck remains immersive without sacrificing contrast.
 */
@Composable
private fun SwipeTopControls(
    onFiltersClick: () -> Unit,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .widthIn(max = CARD_MAX_WIDTH)
                .fillMaxWidth()
                .padding(horizontal = RepoSwipeTheme.spacing.gutter, vertical = RepoSwipeTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwipeActionButton(
            icon = RepoSwipeIcons.Filters,
            contentDescription = stringResource(DesignSystemR.string.topbar_filters_cd),
            onClick = onFiltersClick,
            size = SwipeActionButtonSize.Small,
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
            contentPadding = PaddingValues(end = RepoSwipeTheme.spacing.gutter),
        ) {
            item(key = "for_you") {
                RepoSwipeFilterChip(
                    label = stringResource(R.string.swipe_quick_language_for_you),
                    selected = selectedLanguage == null,
                    onClick = { onLanguageSelected(null) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            items(QuickLanguages, key = { it }) { language ->
                RepoSwipeFilterChip(
                    label = language,
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

/**
 * The deck fills the whole available area (no fixed aspect-ratio box floating mid-screen) — the
 * action row floats over its bottom edge as a glass pill, the same treatment
 * [SwipeTopControls]/[com.batuhan.reposwipe.core.designsystem.component.RepoSwipeBottomNavBar]
 * already use over content elsewhere, rather than reserving its own dedicated strip beneath the
 * card. [SwipeActionButton]'s default translucent-plus-border container is what keeps it legible
 * over whatever color the front card's header photo happens to be.
 */
@Composable
private fun SwipeDeckContent(
    repos: List<Repo>,
    deckState: SwipeDeckState,
    onSwiped: (Repo, SwipeDirection) -> Unit,
    onRewind: () -> Unit,
    onQuickView: (Repo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val frontRepo = repos.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
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
                    .align(Alignment.Center)
                    .widthIn(max = CARD_MAX_WIDTH)
                    .fillMaxSize()
                    .padding(horizontal = RepoSwipeTheme.spacing.gutter),
        ) { repo ->
            // remember, not a bare call: this lambda is re-invoked whenever the deck recomposes,
            // and toCardData does real per-call work (two toCompactCount formats plus a
            // languageColor lookup) to rebuild a value that only changes when `repo` does.
            val cardData = remember(repo) { repo.toCardData() }
            RepoCard(
                data = cardData,
                contentBottomPadding = ACTION_OVERLAY_CLEARANCE,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = CARD_MAX_WIDTH)
                    .fillMaxWidth()
                    .height(ACTION_SCRIM_HEIGHT)
                    .padding(horizontal = RepoSwipeTheme.spacing.gutter)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = CARD_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(
                        horizontal = RepoSwipeTheme.spacing.gutter,
                        vertical = RepoSwipeTheme.spacing.lg,
                    ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = RepoSwipeIcons.Rewind,
                contentDescription = stringResource(R.string.swipe_action_rewind_cd),
                onClick = onRewind,
                size = SwipeActionButtonSize.Medium,
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
                size = SwipeActionButtonSize.Large,
                // Filled red heart rather than the blue brand accent (2026-09-04, user request,
                // matching the Stitch "Rendez-vous" reference's crimson Like button) — kept in
                // sync with SwipeRightOverlay's drag-feedback tint above.
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                borderColor = MaterialTheme.colorScheme.error,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.QuickView,
                contentDescription = stringResource(R.string.swipe_action_quick_view_cd),
                onClick = { frontRepo?.let(onQuickView) },
                size = SwipeActionButtonSize.Medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primaryContainer,
                borderColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            )
        }
    }
}

enum class DetailDeckAction { Rewind, Pass, Star }

private val QuickLanguages =
    AvailableLanguages.filter {
        it in
            setOf("JavaScript", "Python", "Kotlin", "TypeScript", "Rust", "Go", "Swift")
    }
private val ACTION_OVERLAY_CLEARANCE = 104.dp
private val ACTION_SCRIM_HEIGHT = 144.dp
private val CARD_MAX_WIDTH = 560.dp
private val DETAIL_MAX_WIDTH = 720.dp

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

/** Groups independently loading detail sections without widening the screen's parameter list. */
private data class RepoDetailContentState(
    val readme: ReadmeUiState,
    val languageBreakdown: LanguageBreakdownUiState,
    val contributors: ContributorsUiState,
    val similarRepos: SimilarReposUiState,
)

private data class RepoDetailOverviewActions(
    val onReadmeContentHeightMeasured: (Int) -> Unit,
    val onToggleGitHubStar: () -> Unit,
    val onRetryReadme: () -> Unit,
    val onReadmeExpandRequested: () -> Unit,
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
        topics = topics,
    )

/**
 * Full-screen repo detail, opened by tapping the front card or its quick-view button. Two modes,
 * never
 * both on screen at once:
 * - **Overview** ([ReadmeViewMode.Preview]): header/stats/language/contributors/similar-repos,
 *   each its own `LazyColumn` item, plus a capped, non-scrolling README preview that hands taps
 *   off to [ReadmeWebView]'s own `onExpandRequested`.
 * - **Read** ([ReadmeViewMode.Read]): every other section disappears and the same README
 *   [ReadmeWebView] instead fills the page and scrolls internally — the point of the whole
 *   fullscreen-README ask this shipped for: the user reads GitHub's actual rendered README,
 *   images/code-blocks/tables and all, without ever leaving the app.
 *
 * [RepoDetailActionBar] stays pinned at the bottom in *both* modes — a reader who opened the full
 * README is, if anything, more likely to be mid-decision, so taking the star/pass/rewind actions
 * away right when they're most useful would be backwards. It only renders at all when
 * [showDeckActions] is true: Rewind/Pass drive the Discover swipe deck specifically and have no
 * meaning when this screen is opened from a non-deck entry point (Leaderboard, Search) — those
 * callers still get the real GitHub star toggle in the header above.
 *
 * Everything on this page stays in-app except the small icon next to the title and the README's
 * own links — [ReadmeWebView] hands those off to Chrome Custom Tabs, since that content and its
 * links are GitHub's, not this app's, the same as tapping one would do on github.com itself.
 */
@Composable
fun RepoDetailScreen(
    onBack: () -> Unit,
    onDeckAction: (DetailDeckAction) -> Unit,
    showDeckActions: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: RepoDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isStarred by viewModel.isStarred.collectAsStateWithLifecycle()
    // Preview and fullscreen read mode intentionally own different native WebViews. A single
    // Android View cannot safely move between two AndroidView call sites during the same Compose
    // frame: the outgoing preview's onRelease may otherwise detach the View after the reader has
    // already claimed it, leaving a permanently blank reader surface.
    val readmePreviewWebView = rememberReadmeWebView()
    val readmeReaderWebView = rememberReadmeWebView()
    val repo = uiState.repo
    var readmeMode by remember(repo?.id) { mutableStateOf(ReadmeViewMode.Preview) }
    var readmeContentHeightPx by remember(repo?.id) { mutableIntStateOf(0) }
    val readmeHtml = (uiState.readme as? ReadmeUiState.Loaded)?.html
    val isReadMode = readmeMode == ReadmeViewMode.Read && readmeHtml != null
    // Only the swipe-deck entry point's back button actually returns to Discover — Leaderboard/
    // Search/Starred's entry points (showDeckActions = false) return to whichever of those the
    // user came from, so "Back to Discover" would be a misleading TalkBack announcement there.
    val backContentDescription =
        stringResource(if (showDeckActions) R.string.swipe_detail_back_cd else R.string.swipe_detail_back_generic_cd)

    androidx.activity.compose.BackHandler(enabled = isReadMode) { readmeMode = ReadmeViewMode.Preview }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            uiState.isLoading -> {
                RepoDetailTopBar(
                    title = stringResource(R.string.swipe_detail_title),
                    onBack = onBack,
                    backContentDescription = backContentDescription,
                )
                FullScreenState { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            }
            uiState.hasError || repo == null -> {
                RepoDetailTopBar(
                    title = stringResource(R.string.swipe_detail_title),
                    onBack = onBack,
                    backContentDescription = backContentDescription,
                )
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Error,
                        title = stringResource(R.string.swipe_detail_error_title),
                        message = stringResource(R.string.swipe_detail_error_message),
                        iconTint = MaterialTheme.colorScheme.error,
                        actionLabel = stringResource(R.string.swipe_action_retry),
                        onAction = viewModel::retry,
                    )
                }
            }
            else -> {
                val content =
                    RepoDetailContentState(
                        readme = uiState.readme,
                        languageBreakdown = uiState.languageBreakdown,
                        contributors = uiState.contributors,
                        similarRepos = uiState.similarRepos,
                    )

                if (isReadMode) {
                    ReadmeReadModeContent(
                        repo = repo,
                        webView = readmeReaderWebView,
                        html = requireNotNull(readmeHtml),
                        onBack = { readmeMode = ReadmeViewMode.Preview },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    RepoDetailTopBar(
                        title = "${repo.ownerLogin}/${repo.name}",
                        onBack = onBack,
                        backContentDescription = backContentDescription,
                    )
                    RepoDetailOverviewContent(
                        repo = repo,
                        content = content,
                        isStarredOnGitHub = isStarred,
                        webView = readmePreviewWebView,
                        readmeContentHeightPx = readmeContentHeightPx,
                        actions =
                            RepoDetailOverviewActions(
                                onReadmeContentHeightMeasured = { readmeContentHeightPx = it },
                                onToggleGitHubStar = viewModel::toggleStar,
                                onRetryReadme = viewModel::retryReadme,
                                onReadmeExpandRequested = { readmeMode = ReadmeViewMode.Read },
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = DETAIL_MAX_WIDTH),
                    )
                }

                if (showDeckActions) {
                    RepoDetailActionBar(
                        onRewind = { onDeckAction(DetailDeckAction.Rewind) },
                        onReject = { onDeckAction(DetailDeckAction.Pass) },
                        onStar = { onDeckAction(DetailDeckAction.Star) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepoDetailTopBar(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RepoSwipeTheme.spacing.xs, vertical = RepoSwipeTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = RepoSwipeIcons.Back,
                contentDescription = backContentDescription,
            )
        }
        Text(
            text = title,
            style = RepoSwipeTheme.typography.bodyLg.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Read mode's whole body: a minimal back-to-overview title row above a fullscreen, internally
 * scrolling [ReadmeWebView]. Deliberately not the full [RepoDetailHeader] — repeating the
 * star/share/open-external row here would just duplicate controls already one tap away in
 * Overview, for a title bar whose only job is "what am I reading and how do I leave". */
@Composable
private fun ReadmeReadModeContent(
    repo: Repo,
    webView: ScrollGatedWebView,
    html: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RepoSwipeTheme.spacing.gutter, vertical = RepoSwipeTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = RepoSwipeIcons.Back,
                    contentDescription = stringResource(R.string.swipe_detail_readme_back_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${repo.ownerLogin}/${repo.name}",
                style = RepoSwipeTheme.typography.bodyLg.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        ReadmeWebView(
            webView = webView,
            html = html,
            mode = ReadmeViewMode.Read,
            modifier = Modifier.weight(1f).padding(horizontal = RepoSwipeTheme.spacing.gutter),
        )
    }
}

/** Overview mode's whole body — header/stats through similar-repos, README preview placed right
 * after the stats (see [ReadmeSection]'s doc for why it leads instead of trailing). `LazyColumn`
 * rather than a plain scrolling `Column`: the sections below the fold (contributors/similar-repos
 * avatar rows, and especially the README's WebView — real Android View inflation, one of the more
 * expensive things this sheet does) would otherwise compose unconditionally the instant the sheet
 * opened, competing with its own slide-up animation for the frame budget. Composing lazily defers
 * that cost until the user actually scrolls each section into view. */
@Composable
private fun RepoDetailOverviewContent(
    repo: Repo,
    content: RepoDetailContentState,
    isStarredOnGitHub: Boolean?,
    webView: ScrollGatedWebView,
    readmeContentHeightPx: Int,
    actions: RepoDetailOverviewActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
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
                onToggleGitHubStar = actions.onToggleGitHubStar,
            )
        }

        val language = repo.language
        if (language != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
                ) {
                    val dotColor = languageColor(language) ?: MaterialTheme.colorScheme.outline
                    Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
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

        item {
            ReadmeSection(
                state = content.readme,
                webView = webView,
                contentHeightPx = readmeContentHeightPx,
                onContentHeightMeasured = actions.onReadmeContentHeightMeasured,
                onRetry = actions.onRetryReadme,
                onExpandRequested = actions.onReadmeExpandRequested,
            )
        }
        item { LanguageBreakdownSection(state = content.languageBreakdown) }
        item { ContributorsSection(state = content.contributors) }
        item { SimilarReposSection(state = content.similarRepos) }
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
            IconButton(
                onClick = onToggleGitHubStar,
                enabled = isStarredOnGitHub != null,
            ) {
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

private const val MAX_LANGUAGE_LEGEND_ENTRIES = 6
private const val MIN_LANGUAGE_SEGMENT_WEIGHT = 0.001f
private const val LANGUAGE_BAR_HEIGHT_DP = 8
private const val PERCENT_SCALE = 100
private const val CONTRIBUTOR_AVATAR_SIZE_DP = 48
private const val CONTRIBUTOR_AVATAR_COLUMN_WIDTH_DP = 56
private const val SIMILAR_REPO_CARD_WIDTH_DP = 160

/**
 * A capped, non-scrolling preview — tapping anywhere on it, or its "Show more" button once
 * there's genuinely more to read, calls [onExpandRequested], which [RepoDetailSheet] turns into a
 * full mode switch to [ReadmeViewMode.Read]. Placed first among the sheet's sections (right after
 * the header/stats, ahead of language/contributors/similar-repos): the README is the single
 * richest "should I star this?" signal a repo has, and this sheet exists to answer exactly that
 * question — burying it below lower-value sections made the reader do the most work to reach the
 * most useful content.
 */
@Composable
private fun ReadmeSection(
    state: ReadmeUiState,
    webView: ScrollGatedWebView,
    contentHeightPx: Int,
    onContentHeightMeasured: (Int) -> Unit,
    onRetry: () -> Unit,
    onExpandRequested: () -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs)) {
                    Text(
                        text = stringResource(R.string.swipe_detail_readme_error),
                        style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.swipe_action_retry))
                    }
                }
            is ReadmeUiState.Loaded -> {
                val html = state.html
                if (html.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.swipe_detail_readme_empty),
                        style = RepoSwipeTheme.typography.bodySm.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ReadmeWebView(
                        webView = webView,
                        html = html,
                        mode = ReadmeViewMode.Preview,
                        modifier = Modifier.fillMaxWidth(),
                        onExpandRequested = onExpandRequested,
                        onContentHeightMeasured = onContentHeightMeasured,
                    )

                    // Only worth offering once a real measurement has landed and it actually
                    // exceeds the preview's own cap — a short README is fully visible already.
                    val capPx = with(LocalDensity.current) { README_PREVIEW_CAP_DP.dp.roundToPx() }
                    if (contentHeightPx > capPx) {
                        TextButton(onClick = onExpandRequested) {
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

/** Sticky footer inside [RepoDetailScreen] — same buttons/styling as the Discover action row
 * (minus quick-view, since you're already looking at the detail). No GitHub/external-nav button
 * here on purpose: the small icon next to the title is the only way out to GitHub. */
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
                size = SwipeActionButtonSize.Small,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Skip,
                contentDescription = stringResource(R.string.swipe_action_pass),
                onClick = onReject,
                size = SwipeActionButtonSize.Small,
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Like,
                contentDescription = stringResource(R.string.swipe_action_star),
                onClick = onStar,
                size = SwipeActionButtonSize.Medium,
                // Matches the Discover screen's Like button — see its own comment.
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                borderColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}
