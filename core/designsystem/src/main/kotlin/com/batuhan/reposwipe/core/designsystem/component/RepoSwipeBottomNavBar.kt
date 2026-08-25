package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

data class RepoSwipeNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/** Floating pill-shaped dock — the active tab gets its own soft tinted pill inside it. */
@Composable
fun RepoSwipeBottomNavBar(
    items: List<RepoSwipeNavItem>,
    currentRoute: String?,
    onItemClick: (RepoSwipeNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier =
            modifier
                // The dock floats detached from the screen edge (rounded pill, side/bottom
                // margins) — without consuming the navigation-bar inset first, that margin is
                // measured from the physical screen edge instead of the gesture-bar-safe area,
                // so the pill renders cramped against (or behind) the system gesture indicator.
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = RepoSwipeTheme.spacing.lg,
                    end = RepoSwipeTheme.spacing.lg,
                    bottom = RepoSwipeTheme.spacing.xl,
                ).fillMaxWidth()
                .height(64.dp)
                // Lifts the dock visibly off whatever content scrolls beneath it — without this,
                // the bar's own translucent background reads as flush with the screen instead of
                // a separate floating element.
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.35f),
                ).clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f), shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            // A soft tinted pill rather than a solid opaque fill — the accent color itself carries
            // the selected state instead of switching to white-on-purple, matching the tinted
            // "selected" treatment RepoSwipeFilterChip's outlined variant already uses elsewhere.
            val contentColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            val backgroundColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                }

            val itemShape = RoundedCornerShape(percent = 50)
            Column(
                modifier =
                    Modifier
                        // Without this clip, clickable()'s ripple ignores the pill shape below it
                        // and draws across the item's full rectangular bounds — a hard-edged
                        // rectangle poking out past the rounded selected/pressed background.
                        .clip(itemShape)
                        .background(backgroundColor, itemShape)
                        .clickable { onItemClick(item) }
                        .padding(horizontal = RepoSwipeTheme.spacing.sm, vertical = RepoSwipeTheme.spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = item.label,
                    // The mockup renders this label at `scale-75` (label-sm × 0.75 ≈ 9sp) —
                    // at full label-sm(12sp) size, "Profile"/"Discover" wrap to 2 lines inside
                    // the dock's per-item width and get clipped by the bar's fixed 64dp height.
                    style = RepoSwipeTheme.typography.labelMd.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
