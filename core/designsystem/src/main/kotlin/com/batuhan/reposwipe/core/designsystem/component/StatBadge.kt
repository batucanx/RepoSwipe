package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/** Compact icon+value pill overlaid on repo card imagery (star count, fork count, ...). */
@Composable
fun StatBadge(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    contentColor: Color = Color.White,
    containerColor: Color = Color.Black.copy(alpha = 0.6f),
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier =
            modifier
                .background(containerColor, shape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
                .padding(horizontal = RepoSwipeTheme.spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.base),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier)
        Text(text = value, style = RepoSwipeTheme.typography.statsNumber, color = contentColor)
    }
}
