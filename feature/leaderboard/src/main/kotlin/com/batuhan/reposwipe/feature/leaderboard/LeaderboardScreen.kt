package com.batuhan.reposwipe.feature.leaderboard

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.data.model.LeaderboardEntry
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.theme.languageColor

private val RankGradient = Brush.linearGradient(listOf(Color(0xFF0969DA), Color(0xFF7E57C5)))

@Composable
fun LeaderboardScreen(
    onFiltersClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(onMenuClick = onMenuClick, onFiltersClick = onFiltersClick)

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
                LazyColumn(
                    contentPadding = PaddingValues(RepoSwipeTheme.spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                ) {
                    item { LeaderboardHeader() }

                    if (uiState.entries.isEmpty()) {
                        item {
                            EmptyState(
                                icon = RepoSwipeIcons.Leaderboard,
                                title = stringResource(R.string.leaderboard_empty_title),
                                message = stringResource(R.string.leaderboard_empty_message),
                            )
                        }
                    } else {
                        itemsIndexed(uiState.entries, key = { _, entry -> entry.repoId }) { index, entry ->
                            LeaderboardRow(
                                rank = index + 1,
                                entry = entry,
                                onOpenGitHub = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.htmlUrl)))
                                },
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
                                            Text(
                                                text = stringResource(R.string.leaderboard_load_more),
                                                style = RepoSwipeTheme.typography.headlineMd,
                                            )
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
            style = RepoSwipeTheme.typography.displayLg,
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
    onOpenGitHub: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(onClick = onOpenGitHub)
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
                    style = RepoSwipeTheme.typography.headlineMd,
                    color =
                        if (rank == 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
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
                        text = entry.swipeCount.toInt().toCompactCount(),
                        style = RepoSwipeTheme.typography.statsNumber,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
private fun RankBadge(rank: Int) {
    val shape = MaterialTheme.shapes.medium
    val badgeModifier =
        if (rank == 1) {
            Modifier.background(RankGradient, shape)
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
        }
    Box(
        modifier = Modifier.size(40.dp).then(badgeModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = RepoSwipeTheme.typography.statsNumber,
            color = if (rank == 1) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}
