package com.batuhan.reposwipe.feature.followers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.UserListItem
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun PeopleScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeopleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        PeopleTopBar(onClose = onClose)
        PeopleTabRow(selectedTab = uiState.selectedTab, onSelectTab = viewModel::selectTab)

        when {
            uiState.isLoading ->
                FullScreenState {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            uiState.error != null ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Error,
                        title = stringResource(R.string.people_error_title),
                        message = uiState.error?.asString().orEmpty(),
                        iconTint = MaterialTheme.colorScheme.error,
                        actionLabel = stringResource(R.string.people_action_retry),
                        onAction = viewModel::retry,
                    )
                }
            else -> {
                val isFollowersTab = uiState.selectedTab == PeopleTab.FOLLOWERS
                val items = if (isFollowersTab) uiState.followers else uiState.following
                val hasMore = if (isFollowersTab) uiState.followersHasMore else uiState.followingHasMore

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(RepoSwipeTheme.spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                    ) {
                        if (items.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = RepoSwipeIcons.Profile,
                                    title =
                                        stringResource(
                                            if (isFollowersTab) {
                                                R.string.people_empty_followers_title
                                            } else {
                                                R.string.people_empty_following_title
                                            },
                                        ),
                                    message =
                                        stringResource(
                                            if (isFollowersTab) {
                                                R.string.people_empty_followers_message
                                            } else {
                                                R.string.people_empty_following_message
                                            },
                                        ),
                                )
                            }
                        } else {
                            items(items, key = { it.login }) { user ->
                                UserListItem(
                                    data = user,
                                    onToggleFollow = { viewModel.toggleFollow(user) },
                                )
                            }

                            if (hasMore) {
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
                                            Text(stringResource(R.string.people_load_more))
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
private fun PeopleTopBar(onClose: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = RepoSwipeTheme.spacing.xs, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = RepoSwipeIcons.Close,
                contentDescription = stringResource(R.string.people_close_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.people_title),
            style = RepoSwipeTheme.typography.displaySmMobile,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PeopleTabRow(
    selectedTab: PeopleTab,
    onSelectTab: (PeopleTab) -> Unit,
) {
    SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
        Tab(
            selected = selectedTab == PeopleTab.FOLLOWERS,
            onClick = { onSelectTab(PeopleTab.FOLLOWERS) },
            text = { Text(stringResource(R.string.people_tab_followers)) },
        )
        Tab(
            selected = selectedTab == PeopleTab.FOLLOWING,
            onClick = { onSelectTab(PeopleTab.FOLLOWING) },
            text = { Text(stringResource(R.string.people_tab_following)) },
        )
    }
}

@Composable
private fun FullScreenState(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
