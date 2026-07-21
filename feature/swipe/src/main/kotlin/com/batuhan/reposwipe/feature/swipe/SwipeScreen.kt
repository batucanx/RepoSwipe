package com.batuhan.reposwipe.feature.swipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val rateLimit by viewModel.rateLimit.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(onMenuClick = {}, onFiltersClick = onFiltersClick)

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

        if (rateLimit?.isExhausted == true) {
            FullScreenState {
                EmptyState(
                    icon = RepoSwipeIcons.RateLimited,
                    title = "GitHub API sınırına ulaşıldı",
                    message = "${rateLimit!!.resetEpochSeconds.toClockTimeLabel()} civarında tekrar dene.",
                )
            }
        } else {
            when (repos.loadState.refresh) {
                is LoadState.Loading -> FullScreenState { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is LoadState.Error ->
                    FullScreenState {
                        EmptyState(
                            icon = RepoSwipeIcons.Error,
                            title = "Repolar yüklenemedi",
                            message = "Bağlantını kontrol et ve tekrar dene.",
                            iconTint = MaterialTheme.colorScheme.error,
                            actionLabel = "Tekrar Dene",
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
                                    title = "Sonuç bulunamadı",
                                    message = "Bu kritere uyan repo yok — farklı bir dil dene.",
                                )
                            } else {
                                EmptyState(
                                    icon = RepoSwipeIcons.Apply,
                                    title = "Hepsini gördün!",
                                    message = "Bu dildeki tüm repoları kaydırdın — farklı bir dil dene.",
                                )
                            }
                        }
                    } else {
                        SwipeDeckContent(
                            repos = visibleRepos,
                            onSwiped = { repo, direction -> viewModel.onSwiped(repo, direction) },
                            onRewind = viewModel::onRewind,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeDeckContent(
    repos: List<Repo>,
    onSwiped: (Repo, SwipeDirection) -> Unit,
    onRewind: () -> Unit,
) {
    val deckState = rememberSwipeDeckState()
    val coroutineScope = rememberCoroutineScope()

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
            leftActionLabel = "Pas geç",
            rightActionLabel = "Star ver",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
        ) { repo ->
            RepoCard(data = repo.toCardData(), modifier = Modifier.fillMaxSize())
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
                contentDescription = "Geri al",
                onClick = onRewind,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Skip,
                contentDescription = "Pas geç",
                onClick = { coroutineScope.launch { deckState.swipeLeft() } },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.error,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.Like,
                contentDescription = "Star ver",
                onClick = { coroutineScope.launch { deckState.swipeRight() } },
                size = SwipeActionButtonSize.Large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                borderColor = MaterialTheme.colorScheme.primaryContainer,
            )
            SwipeActionButton(
                icon = RepoSwipeIcons.QuickView,
                contentDescription = "Hızlı bakış",
                // reserved for a future repo-detail sheet
                onClick = {},
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

private fun Repo.toCardData(): RepoCardData =
    RepoCardData(
        ownerAvatarUrl = ownerAvatarUrl,
        ownerLogin = ownerLogin,
        name = name,
        description = description,
        headerImageUrl = headerImageUrl,
        starCount = starCount.toCompactCount(),
        forkCount = forkCount.toCompactCount(),
        updatedAtLabel = "Updated ${updatedAt.toRelativeTimeLabel()}",
        languageName = language,
        languageColor = languageColor(language),
    )
