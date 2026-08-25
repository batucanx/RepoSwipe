package com.batuhan.reposwipe.core.designsystem.component

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import androidx.compose.foundation.layout.Column as ColumnLayout

data class UserListItemData(
    val login: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)

/** Glass-card list row for Followers/Following — avatar+name/@login, trailing follow toggle. */
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
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${data.login}",
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.sm))

        if (data.isFollowing) {
            OutlinedButton(onClick = onToggleFollow, shape = MaterialTheme.shapes.extraLarge) {
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
