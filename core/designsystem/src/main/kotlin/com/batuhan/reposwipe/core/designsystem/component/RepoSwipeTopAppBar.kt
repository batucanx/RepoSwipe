package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.R
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.modifier.bottomHairline
import com.batuhan.reposwipe.core.designsystem.theme.BrandAccent
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun RepoSwipeTopAppBar(
    onMenuClick: () -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.topbar_title_default),
    trailingIcon: ImageVector = RepoSwipeIcons.Filters,
    trailingContentDescription: String = stringResource(R.string.topbar_filters_cd),
    // Optional second trailing action, e.g. Discover's search entry point — defaults to absent so
    // every existing call site (Leaderboard/Starred/...) renders exactly as before.
    secondaryTrailingIcon: ImageVector? = null,
    secondaryTrailingContentDescription: String? = null,
    onSecondaryTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .bottomHairline(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = RepoSwipeIcons.Menu,
                contentDescription = stringResource(R.string.topbar_menu_cd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedBrandTitle(text = title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (secondaryTrailingIcon != null && onSecondaryTrailingClick != null) {
                IconButton(onClick = onSecondaryTrailingClick) {
                    Icon(
                        imageVector = secondaryTrailingIcon,
                        contentDescription = secondaryTrailingContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onTrailingClick) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The "RepoSwipe" wordmark — the one piece of branding that's on screen at all times, so it gets a
 * slow, low-contrast gradient sweep through the brand purple ([BrandAccent]) instead of sitting as
 * flat text. Paced as a long pause between short sweeps rather than a continuous scan: this lives
 * in the permanent top bar on every screen, so anything louder/faster would turn into a nag rather
 * than a nice detail. `textWidthPx` (measured via [onSizeChanged], 0f until the first layout pass)
 * keeps the highlight band's travel distance matched to the actual rendered text instead of a
 * guessed constant, so it works the same regardless of locale/font-scale changing the title's width.
 */
@Composable
private fun AnimatedBrandTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    val bandWidthPx = with(LocalDensity.current) { SHIMMER_BAND_WIDTH_DP.dp.toPx() }
    val infiniteTransition = rememberInfiniteTransition(label = "brandTitleShimmer")
    val sweepX by
        infiniteTransition.animateFloat(
            initialValue = -bandWidthPx,
            targetValue = textWidthPx + bandWidthPx,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = SHIMMER_CYCLE_MS
                            -bandWidthPx at 0
                            -bandWidthPx at SHIMMER_PAUSE_MS using LinearEasing
                            (textWidthPx + bandWidthPx) at SHIMMER_CYCLE_MS using FastOutSlowInEasing
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "brandTitleSweepX",
        )

    val shimmerBrush =
        Brush.linearGradient(
            colors = listOf(BrandAccent, ShimmerHighlight, BrandAccent),
            start = Offset(sweepX - bandWidthPx, 0f),
            end = Offset(sweepX + bandWidthPx, 0f),
        )

    Text(
        text = text,
        style = RepoSwipeTheme.typography.displaySmMobile.copy(brush = shimmerBrush),
        modifier = modifier.onSizeChanged { size -> textWidthPx = size.width.toFloat() },
    )
}

// A pale lavender rather than plain white — keeps the sweep's highlight inside the purple family
// instead of reading as a generic white glare crossing the text.
private val ShimmerHighlight = Color(0xFFE3D3FF)

private const val SHIMMER_BAND_WIDTH_DP = 70
private const val SHIMMER_CYCLE_MS = 3600
private const val SHIMMER_PAUSE_MS = 2200
