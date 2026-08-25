package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val languageName: String? = null,
    val languageColor: Color? = null,
)

private val OwnerBadgeSize = 80.dp
private val OwnerBadgeBorder = 4.dp

/**
 * The primary swipe-deck card: header image (or language-tinted fallback), an owner-avatar badge
 * floating over the header/body seam, then name/description/metadata body. Sizing is left to the
 * caller (e.g. `Modifier.aspectRatio(4f / 5f)`).
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
                .clip(shape)
                // Unlike the other glass panels in this app, this card sits directly in front of
                // another full card in the swipe deck (not just a flat background) — the 0.4
                // alpha other "glass-card" mockups use let that stacked card's own text bleed
                // through, so this one stays near-opaque instead.
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RepoCardHeader(
                data = data,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.85f),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1.15f),
            ) {
                RepoCardBody(
                    data = data,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = RepoSwipeTheme.spacing.lg,
                                end = RepoSwipeTheme.spacing.lg,
                                bottom = RepoSwipeTheme.spacing.lg,
                                top = OwnerBadgeSize / 2 + RepoSwipeTheme.spacing.xs,
                            ),
                )
                RepoCardOwnerBadge(
                    data = data,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = RepoSwipeTheme.spacing.lg, y = -(OwnerBadgeSize / 2)),
                )
            }
        }
    }
}

@Composable
private fun RepoCardHeader(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    // The language-tinted fallback is the *background* of this Box, not an if/else alternative
    // to the image — headerImageUrl is always a non-null GitHub OpenGraph URL in practice (see
    // RepoMappers), so the old `if (headerImageUrl != null) image else tint` never actually hit
    // the tint branch. When that OpenGraph image is slow or fails to load, Coil's AsyncImage
    // renders fully transparent (no placeholder/error set) with nothing behind it — invisible in
    // dark mode (near-black bleeds into near-black) but a jarring blank-white gap in light mode.
    // Painting the tint unconditionally first means a failed/loading image reveals the language
    // color instead of a hole through to the screen background.
    Box(
        modifier =
            modifier
                .background(data.languageColor ?: MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (data.headerImageUrl != null) {
            AsyncImage(
                model = data.headerImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // GitHub's auto-generated OpenGraph image is itself a little rendered "card" (owner
        // avatar, repo name, description, stat pills baked into the pixels) — shown at full
        // strength it visually duplicates/competes with our own name/description/stats below,
        // reading as two overlapping card designs. A constant scrim over the top ~60% mutes that
        // baked-in content down to background texture, then the same gradient blends into the
        // body panel's surfaceContainer tone at the bottom — the body sits on the outer card
        // Box's surfaceContainer background, so ending anywhere else leaves a color-mismatched
        // seam where the image meets the body instead of a seamless blend.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.32f),
                            0.6f to Color.Black.copy(alpha = 0.32f),
                            1f to MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(RepoSwipeTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatBadge(icon = RepoSwipeIcons.Star, value = data.starCount)
            StatBadge(icon = RepoSwipeIcons.Fork, value = data.forkCount)
        }
    }
}

@Composable
private fun RepoCardOwnerBadge(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(OwnerBadgeSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(OwnerBadgeBorder, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (data.ownerAvatarUrl != null) {
            AsyncImage(
                model = data.ownerAvatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = RepoSwipeIcons.RepoPlaceholder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
private fun RepoCardBody(
    data: RepoCardData,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = data.name,
            style = RepoSwipeTheme.typography.headlineLgMobile.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = data.ownerLogin,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = RepoSwipeTheme.spacing.base),
        )
        Text(
            text = data.description,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = RepoSwipeTheme.spacing.xs),
        )

        // Star/fork live as badges on the header image now — this row is just the language, kept
        // quiet and out of the way of the name/description above it.
        if (data.languageName != null) {
            MetadataItem(
                dotColor = data.languageColor ?: MaterialTheme.colorScheme.outline,
                label = data.languageName,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    color: Color,
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
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
        }
        Text(text = label, style = RepoSwipeTheme.typography.labelMd, color = color)
    }
}
