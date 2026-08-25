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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val shape = MaterialTheme.shapes.medium
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
            model = avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.size(RepoSwipeTheme.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                // headlineMd(28sp) reads fine for a short mockup name, but real GitHub display
                // names run longer (e.g. three words) and were wrapping to 3 lines here — sized
                // down and capped so any real name still fits cleanly.
                style = RepoSwipeTheme.typography.headlineMd.copy(fontSize = 20.sp, lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@$username",
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statValue,
                style = RepoSwipeTheme.typography.headlineMd,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = statLabel.uppercase(),
                style = RepoSwipeTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
