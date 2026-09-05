package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.CardBackgroundNavy
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import androidx.compose.foundation.layout.Column as ColumnLayout

data class UserListItemData(
    val login: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)

/**
 * List row for Followers/Following — avatar+name/@login, trailing follow toggle.
 *
 * Fixed [CardBackgroundNavy] rather than the theme-driven `surfaceContainer` glass treatment other
 * rows use — matching [RepoCard]/[RepoListItem], which read a pale "whitish" card on light theme's
 * near-white page otherwise. Text/buttons below use fixed light tones for the same reason:
 * `primary`/`secondary` flip dark-on-light in light mode and would go illegible on a card that no
 * longer flips with them.
 */
@Composable
fun UserListItem(
    data: UserListItemData,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navyCardSurface(shape)
                .padding(RepoSwipeTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = data.avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))

        ColumnLayout(modifier = Modifier.weight(1f)) {
            Text(
                text = data.displayName ?: data.login,
                style = RepoSwipeTheme.typography.bodyLg,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${data.login}",
                style = RepoSwipeTheme.typography.labelMd,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.sm))

        if (data.isFollowing) {
            OutlinedButton(
                onClick = onToggleFollow,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            ) {
                Icon(
                    imageVector = RepoSwipeIcons.FollowRemove,
                    contentDescription = stringResource(R.string.user_list_item_unfollow_cd),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.user_list_item_following),
                    modifier = Modifier.padding(start = RepoSwipeTheme.spacing.base),
                )
            }
        } else {
            Button(
                onClick = onToggleFollow,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CardBackgroundNavy),
            ) {
                Icon(
                    imageVector = RepoSwipeIcons.FollowAdd,
                    contentDescription = stringResource(R.string.user_list_item_follow_cd),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.user_list_item_follow),
                    modifier = Modifier.padding(start = RepoSwipeTheme.spacing.base),
                )
            }
        }
    }
}
