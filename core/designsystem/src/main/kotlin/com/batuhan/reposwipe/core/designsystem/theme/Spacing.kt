package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** DESIGN.md spacing scale (dark theme values — the canonical 4px-base scale). */
data class RepoSwipeSpacing(
    val base: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val safeArea: Dp = 24.dp,
)

val LocalRepoSwipeSpacing = staticCompositionLocalOf { RepoSwipeSpacing() }
