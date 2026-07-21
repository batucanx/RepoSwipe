@file:OptIn(ExperimentalTextApi::class)

package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.batuhan.reposwipe.core.designsystem.R

/**
 * Manrope is bundled as a single variable font (res/font/manrope.ttf, wght axis 200-800).
 * Each entry below pins one static instance via [FontVariation] so [FontWeight] selection
 * in [TextStyle]s resolves to the correct rendered weight.
 */
val ManropeFontFamily =
    FontFamily(
        Font(
            resId = R.font.manrope,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            resId = R.font.manrope,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            resId = R.font.manrope,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            resId = R.font.manrope,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
        Font(
            resId = R.font.manrope,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(800)),
        ),
    )
