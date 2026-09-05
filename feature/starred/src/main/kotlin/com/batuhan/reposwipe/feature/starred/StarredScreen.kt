package com.batuhan.reposwipe.feature.starred

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.common.share.shareRepoIntent
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoListItem
import com.batuhan.reposwipe.core.designsystem.component.RepoListItemData
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeFilterChip
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.UserProfileHeader
import com.batuhan.reposwipe.core.designsystem.component.collectIsScrollingDownAsState
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor

@Composable
fun StarredScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
    onOpenDetail: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarredViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val transientErrorMessage = uiState.transientError?.asString()
    val listState = rememberLazyListState()
    val isScrollingDown by listState.collectIsScrollingDownAsState()

    LaunchedEffect(uiState.transientError) {
        if (transientErrorMessage != null) {
            snackbarHostState.showSnackbar(transientErrorMessage)
            viewModel.consumeTransientError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The whole bar collapses away (not just its logo) so the list below actually gains
            // that screen space while scrolled, instead of the bar staying the same height with
            // an empty gap where the logo used to be.
            AnimatedVisibility(
                visible = !isScrollingDown || listState.firstVisibleItemIndex == 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RepoSwipeTopAppBar(onMenuClick = onMenuClick, onTrailingClick = onFiltersClick)
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
                            title = stringResource(R.string.starred_error_title),
                            message = uiState.error?.asString().orEmpty(),
                            iconTint = MaterialTheme.colorScheme.error,
                            actionLabel = stringResource(R.string.starred_action_retry),
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
                            contentPadding = PaddingValues(RepoSwipeTheme.spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                        ) {
                            item {
                                UserProfileHeader(
                                    avatarUrl = uiState.user?.avatarUrl,
                                    displayName = uiState.user?.name ?: uiState.user?.login.orEmpty(),
                                    username = uiState.user?.login.orEmpty(),
                                    statValue = uiState.loadedCount.toString(),
                                    statLabel = stringResource(R.string.starred_stat_label),
                                )
                            }

                            item {
                                LanguageFilterRow(
                                    selectedLanguage = selectedLanguage,
                                    availableLanguages = uiState.availableLanguages,
                                    onSelectLanguage = viewModel::selectLanguage,
                                )
                            }

                            if (uiState.repos.isEmpty()) {
                                item {
                                    EmptyState(
                                        icon = RepoSwipeIcons.Star,
                                        title = stringResource(R.string.starred_empty_title),
                                        message = stringResource(R.string.starred_empty_message),
                                    )
                                }
                            } else {
                                items(uiState.repos, key = { it.id }) { repo ->
                                    RepoListItem(
                                        data = repo.toListItemData(),
                                        onToggleStar = { viewModel.unstar(repo) },
                                        onViewClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl)))
                                        },
                                        onShare = { context.startActivity(shareRepoIntent(repo.htmlUrl)) },
                                        onCardClick = { onOpenDetail(repo.ownerLogin, repo.name) },
                                    )
                                }

                                if (uiState.hasMore) {
                                    item {
                                        if (uiState.isLoadingMore) {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = viewModel::loadMore,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(stringResource(R.string.starred_load_more))
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LanguageFilterRow(
    selectedLanguage: String?,
    availableLanguages: List<String>,
    onSelectLanguage: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm)) {
        item {
            RepoSwipeFilterChip(
                label = stringResource(R.string.starred_all_repos_chip),
                selected = selectedLanguage == null,
                onClick = { onSelectLanguage(null) },
            )
        }
        items(availableLanguages) { language ->
            RepoSwipeFilterChip(
                label = language,
                selected = language == selectedLanguage,
                onClick = { onSelectLanguage(language) },
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

private fun Repo.toListItemData(): RepoListItemData =
    RepoListItemData(
        ownerRepoLabel = "$ownerLogin/$name",
        description = description,
        starCount = starCount.toCompactCount(),
        forkCount = forkCount.toCompactCount(),
        isStarred = true,
        languageName = language,
        languageColor = languageColor(language),
    )
