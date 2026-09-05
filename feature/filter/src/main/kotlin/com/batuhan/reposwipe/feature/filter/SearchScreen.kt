package com.batuhan.reposwipe.feature.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.common.share.shareRepoIntent
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoListItem
import com.batuhan.reposwipe.core.designsystem.component.RepoListItemData
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.modifier.bottomHairline
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor
import com.batuhan.reposwipe.core.designsystem.R as DesignSystemR

@Composable
fun SearchScreen(
    onClose: () -> Unit,
    onOpenDetail: (owner: String, repo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val submittedQuery by viewModel.submittedQuery.collectAsStateWithLifecycle()
    val starredStates by viewModel.starredStates.collectAsStateWithLifecycle()
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        SearchTopBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClose = onClose,
            onSubmit = {
                viewModel.submit()
                keyboardController?.hide()
            },
        )

        when {
            submittedQuery == null ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Search,
                        title = stringResource(R.string.search_prompt_title),
                        message = stringResource(R.string.search_prompt_message),
                    )
                }
            repos.loadState.refresh is LoadState.Loading ->
                FullScreenState { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            repos.loadState.refresh is LoadState.Error ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Error,
                        title = stringResource(R.string.search_error_title),
                        message = stringResource(R.string.search_error_message),
                        iconTint = MaterialTheme.colorScheme.error,
                        actionLabel = stringResource(R.string.search_action_retry),
                        onAction = repos::retry,
                    )
                }
            repos.itemCount == 0 ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.NoResults,
                        title = stringResource(R.string.search_no_results_title),
                        message = stringResource(R.string.search_no_results_message, submittedQuery.orEmpty()),
                    )
                }
            else ->
                LazyColumn(
                    contentPadding = PaddingValues(RepoSwipeTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                ) {
                    items(count = repos.itemCount, key = repos.itemKey { it.id }) { index ->
                        val repo = repos[index] ?: return@items
                        RepoListItem(
                            data = repo.toListItemData(starredStates),
                            onToggleStar = { viewModel.toggleStar(repo) },
                            onViewClick = { onOpenDetail(repo.ownerLogin, repo.name) },
                            onShare = { context.startActivity(shareRepoIntent(repo.htmlUrl)) },
                            viewActionLabel = stringResource(DesignSystemR.string.repo_list_item_view_details),
                            viewActionIcon = RepoSwipeIcons.QuickView,
                        )
                    }
                    if (repos.loadState.append is LoadState.Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .bottomHairline(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = RepoSwipeIcons.Close,
                contentDescription = stringResource(R.string.search_close_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = stringResource(R.string.search_placeholder), style = RepoSwipeTheme.typography.bodySm) },
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        IconButton(onClick = onSubmit) {
            Icon(
                imageVector = RepoSwipeIcons.Search,
                contentDescription = stringResource(R.string.search_submit_cd),
                tint = MaterialTheme.colorScheme.primary,
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

private fun Repo.toListItemData(starredStates: Map<String, Boolean>): RepoListItemData =
    RepoListItemData(
        ownerRepoLabel = "$ownerLogin/$name",
        description = description,
        starCount = starCount.toCompactCount(),
        forkCount = forkCount.toCompactCount(),
        isStarred = starredStates["$ownerLogin/$name"] == true,
        languageName = language,
        languageColor = languageColor(language),
    )
