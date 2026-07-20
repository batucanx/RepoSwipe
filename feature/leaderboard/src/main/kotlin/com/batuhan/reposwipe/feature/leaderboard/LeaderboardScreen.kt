package com.batuhan.reposwipe.feature.leaderboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.batuhan.reposwipe.core.designsystem.component.RepoSwipeTopAppBar

@Composable
fun LeaderboardScreen(onFiltersClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        RepoSwipeTopAppBar(onMenuClick = {}, onFiltersClick = onFiltersClick)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Bugün en çok kaydırılanlar — yakında")
        }
    }
}
