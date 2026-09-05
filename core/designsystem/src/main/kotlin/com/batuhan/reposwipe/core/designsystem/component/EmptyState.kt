package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.BrandAccent
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

/**
 * Consistent "nothing to show" / "something went wrong" block — an icon badge, a title, a
 * one-line explanation, and an optional action (retry, clear filter, ...). Used for every
 * screen's empty-results and error states instead of a bare [Text].
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(RepoSwipeTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RepoSwipeTheme.spacing.md),
    ) {
        // A soft brand-accent glow behind whatever icon/tint the caller passes, rather than a flat
        // gray circle — a consistent bit of "character" shared by every empty/error state.
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(listOf(BrandAccent.copy(alpha = 0.18f), Color.Transparent)),
                        CircleShape,
                    ).border(1.dp, BrandAccent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text = title,
            style = RepoSwipeTheme.typography.headlineMd,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = message,
            style = RepoSwipeTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}
