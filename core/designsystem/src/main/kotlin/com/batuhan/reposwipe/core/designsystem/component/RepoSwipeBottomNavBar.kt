package com.batuhan.reposwipe.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

data class RepoSwipeNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun RepoSwipeBottomNavBar(
    items: List<RepoSwipeNavItem>,
    currentRoute: String?,
    onItemClick: (RepoSwipeNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f))
                .padding(horizontal = RepoSwipeTheme.spacing.gutter, vertical = RepoSwipeTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            val contentColor =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            val backgroundColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                }

            Column(
                modifier =
                    Modifier
                        .background(backgroundColor, RoundedCornerShape(12.dp))
                        .clickable { onItemClick(item) }
                        .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.base),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(imageVector = item.icon, contentDescription = item.label, tint = contentColor)
                Text(text = item.label, style = RepoSwipeTheme.typography.labelMd, color = contentColor)
            }
        }
    }
}
