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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.CardBackgroundNavy
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
    val topics: List<String> = emptyList(),
)

private val OwnerBadgeSize = 80.dp
private val OwnerBadgeBorder = 4.dp
private const val MAX_DESCRIPTION_LINES = 10
private val DESCRIPTION_MIN_FONT_SIZE = 11.sp
private val DESCRIPTION_MAX_FONT_SIZE = 16.sp
private const val MAX_VISIBLE_TOPICS = 3

/**
 * The primary swipe-deck card: header image (or language-tinted fallback), an owner-avatar badge
 * floating over the header/body seam, then name/description/metadata body. Sizing is left to the
 * caller (e.g. `Modifier.aspectRatio(4f / 5f)`).
 */
@Composable
fun RepoCard(
    data: RepoCardData,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier =
            modifier
                .clip(shape)
                // Fixed "Parisian night" navy rather than the theme-driven surfaceContainer — kept
                // identical in dark/light per explicit design request, unlike the other glass
                // panels in this app. Unlike those too, this card sits directly in front of
                // another full card in the swipe deck (not just a flat background) — the 0.4
                // alpha other "glass-card" mockups use let that stacked card's own text bleed
                // through, so this one stays near-opaque instead.
                .navyCardSurface(shape),
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
                    bottomPadding = contentBottomPadding,
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
        // body panel's navy tone at the bottom — the body sits on the outer card Box's
        // CardBackgroundNavy background, so ending anywhere else leaves a color-mismatched seam
        // where the image meets the body instead of a seamless blend.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.32f),
                            0.6f to Color.Black.copy(alpha = 0.32f),
                            1f to CardBackgroundNavy,
                        ),
                    ),
        )
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
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = bottomPadding)) {
        Text(
            text = data.name,
            style = RepoSwipeTheme.typography.headlineLgMobile.copy(fontWeight = FontWeight.Bold),
            // Fixed white rather than colorScheme.primary: that token flips to near-black in light
            // theme (correct for every *other* light surface, illegible against this card's own
            // fixed navy background) — see CardBackgroundNavy's doc comment.
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = data.ownerLogin,
            style = RepoSwipeTheme.typography.bodySm,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = RepoSwipeTheme.spacing.base),
        )
        Text(
            text = data.description,
            // lineHeight in em (not the base style's fixed 24.sp) so it scales down together with
            // autoSize's resolved font size instead of leaving oversized gaps between shrunk lines.
            style = RepoSwipeTheme.typography.bodySm.copy(lineHeight = 1.4.em),
            color = Color.White.copy(alpha = 0.82f),
            // Body text no longer has a fixed size: with the card body now hosting more rows
            // (stat badges, topics, language) than it used to, how much *height* is actually left
            // for the description varies per-card. autoSize picks the biggest font (down to
            // DESCRIPTION_MIN_FONT_SIZE) that still lays the text out without ellipsizing within
            // that leftover space and MAX_DESCRIPTION_LINES, so a long description shrinks to show
            // more of itself instead of always truncating at a fixed size/line-count.
            autoSize = TextAutoSize.StepBased(minFontSize = DESCRIPTION_MIN_FONT_SIZE, maxFontSize = DESCRIPTION_MAX_FONT_SIZE),
            maxLines = MAX_DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = RepoSwipeTheme.spacing.xs),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
            modifier = Modifier.padding(top = RepoSwipeTheme.spacing.sm),
        ) {
            StatBadge(icon = RepoSwipeIcons.Star, value = data.starCount)
            StatBadge(icon = RepoSwipeIcons.Fork, value = data.forkCount)
        }

        if (data.topics.isNotEmpty()) {
            TopicChipRow(
                topics = data.topics,
                modifier = Modifier.padding(top = RepoSwipeTheme.spacing.sm),
            )
        }

        if (data.languageName != null) {
            MetadataItem(
                dotColor = data.languageColor ?: MaterialTheme.colorScheme.outline,
                label = data.languageName,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = RepoSwipeTheme.spacing.sm),
            )
        }
    }
}

/**
 * Read-only topic tags — GitHub's own repo-level labels (`Repo.topics`), the clearest "what
 * category of thing is this" signal short of reading the README itself. Capped at
 * [MAX_VISIBLE_TOPICS]: a repo can carry dozens, and past a handful this stops being a quick scan
 * and starts crowding the card. Non-interactive by design (no `onClick`) — filtering by topic is
 * the dedicated Filter screen's job, not something to duplicate here.
 */
@Composable
private fun TopicChipRow(
    topics: List<String>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        topics.take(MAX_VISIBLE_TOPICS).forEach { topic ->
            Text(
                text = topic,
                style = RepoSwipeTheme.typography.labelMd,
                // Fixed white/translucent-white rather than the theme-driven primary/
                // primaryContainer pair — same reasoning as the body text colors above.
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .clip(shape)
                        .background(Color.White.copy(alpha = 0.12f), shape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
                        .padding(horizontal = RepoSwipeTheme.spacing.sm, vertical = RepoSwipeTheme.spacing.xs),
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
        Text(
            text = label,
            style = RepoSwipeTheme.typography.labelMd,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
