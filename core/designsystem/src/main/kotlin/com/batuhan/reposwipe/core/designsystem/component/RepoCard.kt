package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

data class RepoCardData(
    val ownerAvatarUrl: String?,
    val ownerLogin: String,
    val name: String,
    val description: String,
    val headerImageUrl: String?,
    val starCount: String,
    val forkCount: String,
    val updatedAtLabel: String,
    val languageName: String? = null,
    val languageColor: Color? = null,
)

/**
 * The primary swipe-deck card: header image (or language-tinted fallback) with stat badges,
 * gradient legibility wash, then owner/name/description/metadata body.
 *
 * Sizing is left to the caller (e.g. `Modifier.aspectRatio(3f / 4f)`) so this stays reusable
 * outside the swipe deck too (previews, "quick view").
 */
@Composable
fun RepoCard(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RepoCardHeader(
                data = data,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            RepoCardBody(
                data = data,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(RepoSwipeTheme.spacing.lg),
            )
        }
    }
}

@Composable
private fun RepoCardHeader(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (data.headerImageUrl != null) {
            AsyncImage(
                model = data.headerImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(data.languageColor ?: MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }

        // Bottom fade into the card surface (text legibility) + top wash (badge legibility).
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.3f),
                            0.55f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(RepoSwipeTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatBadge(icon = RepoSwipeIcons.Star, value = data.starCount, iconTint = Color(0xFFF1E05A))
            StatBadge(icon = RepoSwipeIcons.Fork, value = data.forkCount, iconTint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RepoCardBody(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
        ) {
            AsyncImage(
                model = data.ownerAvatarUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column {
                Text(
                    text = data.name,
                    style = RepoSwipeTheme.typography.headlineMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = data.ownerLogin,
                    style = RepoSwipeTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = data.description,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
        ) {
            if (data.languageName != null) {
                MetadataItem(
                    dotColor = data.languageColor ?: MaterialTheme.colorScheme.outline,
                    label = data.languageName,
                )
            }
            MetadataItem(icon = RepoSwipeIcons.UpdatedAt, label = data.updatedAtLabel)
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        when {
            dotColor != null ->
                Box(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .background(dotColor, CircleShape),
                )
            icon != null ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
        }
        Text(
            text = label,
            style = RepoSwipeTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
