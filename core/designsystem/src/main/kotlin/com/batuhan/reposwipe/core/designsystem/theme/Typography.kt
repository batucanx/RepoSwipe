package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale as named in DESIGN.md (dark + light merged into one shared scale — only
 * [androidx.compose.material3.ColorScheme] switches between themes, not type).
 * `headline-md` follows the dark spec's Bold(700) rather than light's SemiBold(600); the
 * difference is small and Bold reads consistently well against both surfaces.
 */
data class RepoSwipeTypography(
    val displayLg: TextStyle,
    val displaySmMobile: TextStyle,
    val headlineXl: TextStyle,
    val headlineLg: TextStyle,
    val headlineLgMobile: TextStyle,
    val headlineMd: TextStyle,
    val bodyLg: TextStyle,
    val bodySm: TextStyle,
    val labelMd: TextStyle,
    val statsNumber: TextStyle,
)

val defaultRepoSwipeTypography =
    RepoSwipeTypography(
        displayLg =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.02).em,
            ),
        displaySmMobile =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.01).em,
            ),
        headlineXl =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.02).em,
            ),
        headlineLg =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.01).em,
            ),
        headlineLgMobile =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        headlineMd =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        bodyLg =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodySm =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMd =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.05.em,
            ),
        statsNumber =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 16.sp,
            ),
    )

val LocalRepoSwipeTypography = staticCompositionLocalOf { defaultRepoSwipeTypography }

/** M3 [Typography] fallback for stock Material components (buttons, text fields, ...). */
val RepoSwipeMaterialTypography: Typography =
    Typography().let { base ->
        base.copy(
            displayLarge = defaultRepoSwipeTypography.displayLg,
            displaySmall = defaultRepoSwipeTypography.displaySmMobile,
            headlineLarge = defaultRepoSwipeTypography.headlineLg,
            headlineMedium = defaultRepoSwipeTypography.headlineMd,
            headlineSmall = defaultRepoSwipeTypography.headlineLgMobile,
            titleLarge = base.titleLarge.copy(fontFamily = ManropeFontFamily),
            titleMedium = base.titleMedium.copy(fontFamily = ManropeFontFamily),
            titleSmall = base.titleSmall.copy(fontFamily = ManropeFontFamily),
            bodyLarge = defaultRepoSwipeTypography.bodyLg,
            bodyMedium = defaultRepoSwipeTypography.bodySm,
            bodySmall = base.bodySmall.copy(fontFamily = ManropeFontFamily),
            labelLarge = base.labelLarge.copy(fontFamily = ManropeFontFamily),
            labelMedium = defaultRepoSwipeTypography.labelMd,
            labelSmall = base.labelSmall.copy(fontFamily = ManropeFontFamily),
        )
    }
