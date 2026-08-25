package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Ultra-rounded scale from lumina_exploration/DESIGN.md (`rounded: sm 8dp, DEFAULT 16dp,
 * md 24dp, lg 32dp, xl 48dp, full pill`). The `full` (pill) token has no direct M3 [Shapes]
 * slot; components that need it use `RoundedCornerShape(percent = 50)` directly.
 */
val RepoSwipeDarkShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(48.dp),
    )
