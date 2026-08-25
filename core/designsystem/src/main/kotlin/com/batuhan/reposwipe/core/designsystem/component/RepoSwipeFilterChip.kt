package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * Pill-shaped selectable chip. Two selected treatments match the mockups:
 * - [outlined] = false (default): Discover's quick-filter bar, selected = solid `primaryContainer` fill.
 * - [outlined] = true: the Filter screen, selected = tinted glass fill/border instead of a solid one.
 */
@Composable
fun RepoSwipeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    leadingDotColor: Color? = null,
) {
    val shape = RoundedCornerShape(percent = 50)
    val containerColor =
        when {
            selected && !outlined -> MaterialTheme.colorScheme.primaryContainer
            selected && outlined -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
        }
    // The filter-sheet's outlined chips keep white label text even when selected — only the
    // tinted fill + border signal selection (see filtreler_reposwipe/code.html: every chip uses
    // `text-primary`).
    val contentColor = if (selected && !outlined) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val borderColor =
        when {
            selected && outlined -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            selected -> Color.Transparent
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        }

    Row(
        modifier =
            modifier
                // Clips clickable()'s ripple to the pill shape below — without it the ripple
                // draws across the chip's full rectangular bounds instead of the rounded pill.
                .clip(shape)
                .background(containerColor, shape)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(leadingDotColor, CircleShape),
            )
        }
        Text(text = label, style = RepoSwipeTheme.typography.labelMd, color = contentColor)
    }
}
