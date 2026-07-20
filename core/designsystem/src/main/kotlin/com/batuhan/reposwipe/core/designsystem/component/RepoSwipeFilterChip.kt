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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * Pill-shaped selectable chip. Two selected treatments match the mockups:
 * - [outlined] = false (default): quick-filter bar, selected = solid `primaryContainer` fill.
 * - [outlined] = true: filter sheet multi-select, selected = tinted outline instead of fill.
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
    val containerColor = when {
        selected && !outlined -> MaterialTheme.colorScheme.primaryContainer
        selected && outlined -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        selected && !outlined -> MaterialTheme.colorScheme.onPrimaryContainer
        selected && outlined -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        selected && outlined -> MaterialTheme.colorScheme.primary
        selected -> Color.Transparent
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(leadingDotColor, CircleShape),
            )
        }
        Text(text = label, style = RepoSwipeTheme.typography.labelMd, color = contentColor)
    }
}
