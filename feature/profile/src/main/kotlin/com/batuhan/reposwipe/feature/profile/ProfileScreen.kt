package com.batuhan.reposwipe.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.common.format.toCompactCount
import com.batuhan.reposwipe.core.common.format.toRelativeTimeLabel
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.designsystem.component.EmptyState
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar
import com.batuhan.reposwipe.core.designsystem.component.navyCardSurface
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.text.asString
import com.batuhan.reposwipe.core.designsystem.theme.BrandAccent
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.core.designsystem.R as DesignSystemR

@Composable
fun ProfileScreen(
    onMenuClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(
            onMenuClick = onMenuClick,
            onTrailingClick = onMenuClick,
            trailingIcon = RepoSwipeIcons.Settings,
            trailingContentDescription = stringResource(DesignSystemR.string.topbar_settings_cd),
        )

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
                    // Scrollable, not pinned-to-bottom-via-weight: with the Recent Repositories
                    // section, total content height can exceed the screen on smaller devices —
                    // a fixed-height Column with a weight(1f) spacer just let the last repo row
                    // get squeezed or clipped instead of laying out.
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(RepoSwipeTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.lg),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_top_bar_title),
                            style = RepoSwipeTheme.typography.headlineLgMobile,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        ProfileIdentityHeader(
                            avatarUrl = user.avatarUrl,
                            displayName = user.name ?: user.login,
                            username = user.login,
                        )

                        ProfileStatsRow(
                            reposValue = user.publicRepos.toCompactCount(),
                            followersValue = user.followers.toCompactCount(),
                            followingValue = user.following.toCompactCount(),
                            onFollowersClick = onFollowersClick,
                            onFollowingClick = onFollowingClick,
                        )

                        if (uiState.recentRepos.isNotEmpty()) {
                            RecentReposSection(
                                repos = uiState.recentRepos,
                                onViewAll = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/${user.login}?tab=repositories"),
                                        ),
                                    )
                                },
                                onOpenRepo = { repo ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl)))
                                },
                            )
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
private fun RecentReposSection(
    repos: List<Repo>,
    onViewAll: () -> Unit,
    onOpenRepo: (Repo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_section_recent_repos).uppercase(),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_view_all),
                    style = RepoSwipeTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = RepoSwipeIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Plain rows separated by hairlines rather than each stacked in its own bordered box —
        // less "framed", reads as one simple list instead of a stack of small cards.
        Column {
            repos.forEachIndexed { index, repo ->
                RecentRepoRow(repo = repo, onClick = { onOpenRepo(repo) })
                if (index != repos.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun RecentRepoRow(
    repo: Repo,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = RepoSwipeTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = repo.name,
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.profile_updated_at, repo.updatedAt.toRelativeTimeLabel()),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Icon(
            imageVector = RepoSwipeIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Avatar + name/username, the screen's one "identity" moment — deliberately not the shared
 * [UserProfileHeader] (used as-is by Starred with its own single trailing stat): Profile wants a
 * bigger, richer treatment that would be out of place on Starred's simpler header. */
@Composable
private fun ProfileIdentityHeader(
    avatarUrl: String?,
    displayName: String,
    username: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(BrandAccent.copy(alpha = 0.10f), Color.Transparent)),
                    MaterialTheme.shapes.medium,
                ).padding(RepoSwipeTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = RepoSwipeTheme.typography.headlineLgMobile.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@$username",
                style = RepoSwipeTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/**
 * Repos/Followers/Following as one continuous strip instead of a header-trailing stat plus a
 * separate 2-box row below it.
 *
 * Fixed navy background ([navyCardSurface]) rather than the theme-driven `surfaceContainer` glass
 * treatment — matching [RepoCard]/[com.batuhan.reposwipe.core.designsystem.component.RepoListItem], which read
 * a pale "whitish" strip on light theme's near-white page otherwise. Text below uses fixed light
 * tones for the same reason: `primaryContainer`/`secondary` flip dark-on-light in light mode and
 * would go illegible on a strip that no longer flips with them.
 */
@Composable
private fun ProfileStatsRow(
    reposValue: String,
    followersValue: String,
    followingValue: String,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // Clips the segment ripples below to the row's own rounded corners — without it
                // the outer segments' rectangular ripple pokes past the rounded border.
                .clip(shape)
                .navyCardSurface(shape)
                .padding(vertical = RepoSwipeTheme.spacing.sm),
    ) {
        ProfileStatColumn(modifier = Modifier.weight(1f), value = reposValue, label = stringResource(R.string.profile_stat_repos))
        ProfileStatDivider()
        ProfileStatColumn(
            modifier = Modifier.weight(1f),
            value = followersValue,
            label = stringResource(R.string.profile_stat_followers),
            onClick = onFollowersClick,
        )
        ProfileStatDivider()
        ProfileStatColumn(
            modifier = Modifier.weight(1f),
            value = followingValue,
            label = stringResource(R.string.profile_stat_following),
            onClick = onFollowingClick,
        )
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier =
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.18f)),
    )
}

@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = RepoSwipeTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Text(
            text = label.uppercase(),
            style = RepoSwipeTheme.typography.labelMd,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}
