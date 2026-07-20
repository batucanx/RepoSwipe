package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun RepoSwipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) RepoSwipeDarkColorScheme else RepoSwipeLightColorScheme
    val shapes = if (darkTheme) RepoSwipeDarkShapes else RepoSwipeLightShapes

    CompositionLocalProvider(
        LocalRepoSwipeSpacing provides RepoSwipeSpacing(),
        LocalRepoSwipeTypography provides defaultRepoSwipeTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RepoSwipeMaterialTypography,
            shapes = shapes,
            content = content,
        )
    }
}

/** Ergonomic access mirroring `MaterialTheme.colorScheme` / `MaterialTheme.typography`. */
object RepoSwipeTheme {
    val typography: RepoSwipeTypography
        @Composable get() = LocalRepoSwipeTypography.current

    val spacing: RepoSwipeSpacing
        @Composable get() = LocalRepoSwipeSpacing.current
}
