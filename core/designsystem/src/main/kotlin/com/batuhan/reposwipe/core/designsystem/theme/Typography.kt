package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale from stitch_ultra_modern_premium_mobile/lumina_exploration/DESIGN.md, which only
 * names 5 roles (headline-xl/headline-lg/headline-lg-mobile/body-md/label-sm). The remaining
 * fields below are legacy slots still referenced across screens (e.g. `RepoCard`'s headlineMd,
 * `RepoSwipeTopAppBar`'s displaySmMobile) mapped onto the closest matching new role rather than
 * renamed everywhere, per the mockups: repo card titles and the top-bar brand both render with
 * `font-headline-lg-mobile` in ke_fet_reposwipe/code.html, and stat badges/metadata render with
 * `font-label-sm`.
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
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.02).em,
            ),
        displaySmMobile =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineXl =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.02).em,
            ),
        headlineLg =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.01).em,
            ),
        headlineLgMobile =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineMd =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        bodyLg =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodySm =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        labelMd =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.05.em,
            ),
        statsNumber =
            TextStyle(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.05.em,
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
            titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSansFontFamily),
            titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSansFontFamily),
            titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSansFontFamily),
            bodyLarge = defaultRepoSwipeTypography.bodyLg,
            bodyMedium = defaultRepoSwipeTypography.bodySm,
            bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSansFontFamily),
            labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSansFontFamily),
            labelMedium = defaultRepoSwipeTypography.labelMd,
            labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSansFontFamily),
        )
    }
