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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * Avatar + name/username + a single trailing stat — used by "My Stars" and the Profile tab.
 *
 * Fixed navy background ([navyCardSurface]) rather than the theme-driven `surfaceContainer` glass
 * treatment other rows use — matching [RepoCard]/[RepoListItem], which read a pale "whitish" card on light theme's
 * near-white page otherwise. Text below is a fixed light tone for the same reason: `primary`/
 * `secondary` flip dark-on-light in light mode and would go illegible on a card that no longer
 * flips with them.
 */
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
                .navyCardSurface(shape)
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
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@$username",
                style = RepoSwipeTheme.typography.labelMd,
                color = Color.White.copy(alpha = 0.72f),
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statValue,
                style = RepoSwipeTheme.typography.headlineMd,
                color = Color.White,
            )
            Text(
                text = statLabel.uppercase(),
                style = RepoSwipeTheme.typography.labelMd,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}
