package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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

/**
 * List row for "My Stars"/search/trending — repo icon+name, star toggle, description, stats, and
 * a primary CTA whose label/icon/action the caller controls (e.g. "View on GitHub" for Starred vs.
 * "View Details" to open the in-app repo detail screen from Search).
 *
 * [onCardClick], when non-null, makes the row itself open the in-app repo detail screen (Starred:
 * "View on GitHub" stays a distinct explicit action for actually leaving the app, while tapping
 * anywhere else on the card opens details in-app) — nested per Compose's normal click-consumption
 * so the CTA button/star/share icons still handle their own taps first. Left null (default) where
 * a caller's CTA button already *is* the "open details" action (Search's "View Details"), so the
 * row isn't redundantly double-clickable to the same destination.
 *
 * Fixed navy background ([navyCardSurface]) rather than the theme-driven `surfaceContainer` glass
 * treatment other list rows use — matching [RepoCard]'s own card, which reads a pale/washed-out
 * "whitish" card on light theme's near-white page otherwise. Same as there, every text/icon color is a fixed
 * light tone rather than the usual `primary`/`secondary`/`onSurface` tokens, since those flip
 * dark-on-light in light mode and would go illegible against a card that no longer flips with them.
 */
@Composable
fun RepoListItem(
    data: RepoListItemData,
    onToggleStar: () -> Unit,
    onViewClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    viewActionLabel: String = stringResource(R.string.repo_list_item_view_on_github),
    viewActionIcon: ImageVector = RepoSwipeIcons.OpenExternal,
    onCardClick: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier =
            modifier
                .clip(shape)
                .navyCardSurface(shape)
                .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier)
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
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RepoSwipeIcons.Repo,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = data.ownerRepoLabel,
                    style = RepoSwipeTheme.typography.bodyLg.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = RepoSwipeIcons.Share,
                        contentDescription = stringResource(R.string.repo_list_item_share_cd),
                        tint = Color.White.copy(alpha = 0.72f),
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
                        tint = Color.White,
                    )
                }
            }
        }

        Text(
            text = data.description,
            style = RepoSwipeTheme.typography.bodySm,
            color = Color.White.copy(alpha = 0.82f),
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
                                .background(data.languageColor ?: Color.White.copy(alpha = 0.5f), CircleShape),
                    )
                    Text(
                        text = data.languageName,
                        style = RepoSwipeTheme.typography.labelMd,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
            }
            StatLabel(icon = RepoSwipeIcons.Star, value = data.starCount)
            StatLabel(icon = RepoSwipeIcons.Fork, value = data.forkCount)
        }

        Button(
            onClick = onViewClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = GitHubBlue),
        ) {
            Text(text = viewActionLabel, color = Color.White)
            Icon(
                imageVector = viewActionIcon,
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
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = value,
            style = RepoSwipeTheme.typography.labelMd,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}
