package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/** Avatar + name/username + a single trailing stat — used by "My Stars" and the Profile tab. */
@Composable
fun UserProfileHeader(
    avatarUrl: String?,
    displayName: String,
    username: String,
    statValue: String,
    statLabel: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .padding(RepoSwipeTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = RepoSwipeTheme.typography.headlineMd,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "@$username",
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statValue,
                style = RepoSwipeTheme.typography.headlineMd,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = statLabel,
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
