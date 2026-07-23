package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

data class RepoListItemData(
    val ownerRepoLabel: String,
    val description: String,
    val starCount: String,
    val forkCount: String,
    val isStarred: Boolean,
    val languageName: String? = null,
    val languageColor: Color? = null,
)

/** Compact list row for "My Stars" — repo icon+name, star toggle, description, stats, CTA. */
@Composable
fun RepoListItem(
    data: RepoListItemData,
    onToggleStar: () -> Unit,
    onOpenGitHub: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(RepoSwipeTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = RepoSwipeIcons.Repo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = data.ownerRepoLabel,
                    style = RepoSwipeTheme.typography.headlineMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = RepoSwipeIcons.Share,
                        contentDescription = stringResource(R.string.repo_list_item_share_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (data.isStarred) RepoSwipeIcons.StarFilled else RepoSwipeIcons.Star,
                        contentDescription =
                            if (data.isStarred) {
                                stringResource(R.string.repo_list_item_unstar_cd)
                            } else {
                                stringResource(R.string.repo_list_item_star_cd)
                            },
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        Text(
            text = data.description,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (data.languageName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .background(data.languageColor ?: MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Text(
                        text = data.languageName,
                        style = RepoSwipeTheme.typography.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatLabel(icon = RepoSwipeIcons.Star, value = data.starCount)
            StatLabel(icon = RepoSwipeIcons.Fork, value = data.forkCount)
        }

        Button(
            onClick = onOpenGitHub,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(text = stringResource(R.string.repo_list_item_view_on_github))
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

@Composable
private fun StatLabel(
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
