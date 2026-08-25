package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * A quiet, list-row-styled destructive action (sign out, delete account, ...) — icon + label in
 * [MaterialTheme.colorScheme.error], no border or fill. Meant to read as a secondary, safe-to-miss
 * action at the end of a screen rather than a CTA-sized button.
 */
@Composable
fun DangerActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = RepoSwipeTheme.spacing.xs, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
