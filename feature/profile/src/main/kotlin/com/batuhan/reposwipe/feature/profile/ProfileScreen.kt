package com.batuhan.reposwipe.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.UserProfileHeader
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(onMenuClick = onMenuClick, onFiltersClick = {}, title = stringResource(R.string.profile_top_bar_title))

        when {
            uiState.isLoading ->
                FullScreenState {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            uiState.error != null ->
                FullScreenState {
                    EmptyState(
                        icon = RepoSwipeIcons.Error,
                        title = stringResource(R.string.profile_error_title),
                        message = uiState.error?.asString().orEmpty(),
                        iconTint = MaterialTheme.colorScheme.error,
                        actionLabel = stringResource(R.string.profile_action_retry),
                        onAction = viewModel::retry,
                    )
                }
            else -> {
                val user = uiState.user
                if (user != null) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(RepoSwipeTheme.spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                    ) {
                        UserProfileHeader(
                            avatarUrl = user.avatarUrl,
                            displayName = user.name ?: user.login,
                            username = user.login,
                            statValue = user.publicRepos.toCompactCount(),
                            statLabel = stringResource(R.string.profile_stat_repos),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
                        ) {
                            ProfileStatTile(
                                modifier = Modifier.weight(1f),
                                value = user.followers.toCompactCount(),
                                label = stringResource(R.string.profile_stat_followers),
                            )
                            ProfileStatTile(
                                modifier = Modifier.weight(1f),
                                value = user.following.toCompactCount(),
                                label = stringResource(R.string.profile_stat_following),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = viewModel::signOut,
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        ) {
                            Icon(
                                imageVector = RepoSwipeIcons.SignOut,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.xs))
                            Text(text = stringResource(R.string.profile_sign_out))
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
private fun ProfileStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(RepoSwipeTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = RepoSwipeTheme.typography.headlineMd,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
