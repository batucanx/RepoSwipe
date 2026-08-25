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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.GitHubBlue
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

/** Glass-card list row for "My Stars" — repo icon+name, star toggle, description, stats, CTA. */
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
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
                .padding(RepoSwipeTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.xs),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RepoSwipeIcons.Repo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = data.ownerRepoLabel,
                    style = RepoSwipeTheme.typography.bodyLg.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = RepoSwipeIcons.Share,
                        contentDescription = stringResource(R.string.repo_list_item_share_cd),
                        tint = MaterialTheme.colorScheme.secondary,
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
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
        }

        Text(
            text = data.description,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
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
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            StatLabel(icon = RepoSwipeIcons.Star, value = data.starCount)
            StatLabel(icon = RepoSwipeIcons.Fork, value = data.forkCount)
        }

        Button(
            onClick = onOpenGitHub,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = GitHubBlue),
        ) {
            Text(text = stringResource(R.string.repo_list_item_view_on_github), color = Color.White)
            Icon(
                imageVector = RepoSwipeIcons.OpenExternal,
                contentDescription = null,
                tint = Color.White,
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
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = value,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
