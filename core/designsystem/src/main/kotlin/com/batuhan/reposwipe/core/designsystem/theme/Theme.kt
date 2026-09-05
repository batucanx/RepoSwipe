package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRepoSwipeDarkTheme = staticCompositionLocalOf { true }

@Composable
fun RepoSwipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRepoSwipeSpacing provides RepoSwipeSpacing(),
        LocalRepoSwipeTypography provides defaultRepoSwipeTypography,
        LocalRepoSwipeDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) RepoSwipeDarkColorScheme else RepoSwipeLightColorScheme,
            typography = RepoSwipeMaterialTypography,
            shapes = RepoSwipeDarkShapes,
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

    val isDarkTheme: Boolean
        @Composable get() = LocalRepoSwipeDarkTheme.current
}
