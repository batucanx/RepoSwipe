package com.batuhan.reposwipe.feature.swipe

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.batuhan.reposwipe.core.common.format.toClockTimeLabel
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.common.format.toRelativeTimeLabel
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoCard
import com.batuhan.reposwipe.core.designsystem.component.RepoCardData
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButton
import com.batuhan.reposwipe.core.designsystem.component.SwipeActionButtonSize
import com.batuhan.reposwipe.core.designsystem.component.SwipeDeck
import com.batuhan.reposwipe.core.designsystem.component.rememberSwipeDeckState
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import kotlinx.coroutines.launch

@Composable
fun SwipeScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()
    var repoForDetail by remember { mutableStateOf<Repo?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(onMenuClick = onMenuClick, onFiltersClick = onFiltersClick)

        LazyRow(
            contentPadding =
                PaddingValues(
                    horizontal = RepoSwipeTheme.spacing.gutter,
                    vertical = RepoSwipeTheme.spacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
        ) {
            items(DiscoverLanguages) { language ->
                RepoSwipeFilterChip(
                    label = language,
                    selected = language in filters.languages,
                    onClick = { viewModel.toggleLanguage(language) },
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
            when (repos.loadState.refresh) {
                is LoadState.Loading -> FullScreenState { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is LoadState.Error ->
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
                    val visibleRepos =
                        (currentIndex until minOf(currentIndex + 3, repos.itemCount))
                            .mapNotNull { index -> repos[index] }

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
        RepoDetailSheet(repo = repo, onDismiss = { repoForDetail = null })
    }
}

@Composable
private fun SwipeDeckContent(
    repos: List<Repo>,
    onSwiped: (Repo, SwipeDirection) -> Unit,
    onRewind: () -> Unit,
    onQuickView: (Repo) -> Unit,
) {
    val deckState = rememberSwipeDeckState()
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
        ) { repo ->
            val updatedLabel = stringResource(R.string.swipe_updated_at, repo.updatedAt.toRelativeTimeLabel())
            RepoCard(data = repo.toCardData(updatedLabel), modifier = Modifier.fillMaxSize())
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
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Skip,
                contentDescription = stringResource(R.string.swipe_action_pass),
                onClick = { coroutineScope.launch { deckState.swipeLeft() } },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Like,
                contentDescription = stringResource(R.string.swipe_action_star),
                onClick = { coroutineScope.launch { deckState.swipeRight() } },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                borderColor = MaterialTheme.colorScheme.primaryContainer,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.QuickView,
                contentDescription = stringResource(R.string.swipe_action_quick_view_cd),
                onClick = { frontRepo?.let(onQuickView) },
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

private fun Repo.toCardData(updatedAtLabel: String): RepoCardData =
    RepoCardData(
        ownerAvatarUrl = ownerAvatarUrl,
        ownerLogin = ownerLogin,
        name = name,
        description = description,
        headerImageUrl = headerImageUrl,
        starCount = starCount.toCompactCount(),
        forkCount = forkCount.toCompactCount(),
        updatedAtLabel = updatedAtLabel,
        languageName = language,
        languageColor = languageColor(language),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoDetailSheet(
    repo: Repo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RepoSwipeTheme.spacing.gutter)
                    .padding(bottom = RepoSwipeTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
        ) {
            Text(
                text = "${repo.ownerLogin}/${repo.name}",
                style = RepoSwipeTheme.typography.headlineMd,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = repo.description,
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val language = repo.language
                if (language != null) {
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
                DetailStat(icon = RepoSwipeIcons.Star, value = repo.starCount.toCompactCount())
                DetailStat(icon = RepoSwipeIcons.Fork, value = repo.forkCount.toCompactCount())
            }

            Text(
                text = stringResource(R.string.swipe_updated_at, repo.updatedAt.toRelativeTimeLabel()),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(text = stringResource(R.string.swipe_detail_view_on_github))
                Icon(
                    imageVector = RepoSwipeIcons.OpenExternal,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(start = RepoSwipeTheme.spacing.base)
                            .size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailStat(
    icon: ImageVector,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = value,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
