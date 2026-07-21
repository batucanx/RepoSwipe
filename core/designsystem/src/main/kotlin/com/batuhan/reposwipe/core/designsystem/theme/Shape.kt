package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md defines a different `rounded` scale per theme (dark: rounder/24px cards,
 * light: tighter/8px cards to read as a "code editor" tool) — so, unusually, shapes
 * switch with [RepoSwipeDarkColorScheme]/[RepoSwipeLightColorScheme] rather than staying
 * theme-invariant. The `full` (pill) token has no direct M3 [Shapes] slot; components
 * that need it use `RoundedCornerShape(percent = 50)` directly.
 */
val RepoSwipeDarkShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

val RepoSwipeLightShapes =
    Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )
