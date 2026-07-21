package com.batuhan.reposwipe.core.designsystem.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.batuhan.reposwipe.core.designsystem.icon.RepoSwipeIcons
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme

@Composable
fun RepoSwipeTopAppBar(
    onMenuClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "RepoSwipe",
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(horizontal = RepoSwipeTheme.spacing.md, vertical = RepoSwipeTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = RepoSwipeIcons.Menu,
                contentDescription = "Menü",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = RepoSwipeTheme.typography.displaySmMobile,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onFiltersClick) {
            Icon(
                imageVector = RepoSwipeIcons.Filters,
                contentDescription = "Filtreler",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
