package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** lumina_exploration/DESIGN.md spacing scale (stack-sm/stack-md/container-padding/margin-page/stack-lg). */
data class RepoSwipeSpacing(
    val base: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 16.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val safeArea: Dp = 24.dp,
)

val LocalRepoSwipeSpacing = staticCompositionLocalOf { RepoSwipeSpacing() }
