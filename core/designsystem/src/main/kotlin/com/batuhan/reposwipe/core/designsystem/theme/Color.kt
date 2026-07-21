package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark theme tokens — dark_themes/reposwipe/DESIGN.md
private val DarkSurface = Color(0xFF10131A)
private val DarkSurfaceDim = Color(0xFF10131A)
private val DarkSurfaceBright = Color(0xFF363941)
private val DarkSurfaceContainerLowest = Color(0xFF0B0E15)
private val DarkSurfaceContainerLow = Color(0xFF191C22)
private val DarkSurfaceContainer = Color(0xFF1D2027)
private val DarkSurfaceContainerHigh = Color(0xFF272A31)
private val DarkSurfaceContainerHighest = Color(0xFF32353C)
private val DarkOnSurface = Color(0xFFE1E2EC)
private val DarkOnSurfaceVariant = Color(0xFFC2C6D6)
private val DarkInverseSurface = Color(0xFFE1E2EC)
private val DarkInverseOnSurface = Color(0xFF2E3038)
private val DarkOutline = Color(0xFF8C909F)
private val DarkOutlineVariant = Color(0xFF424753)
private val DarkPrimary = Color(0xFFADC6FF)
private val DarkOnPrimary = Color(0xFF002E68)
private val DarkPrimaryContainer = Color(0xFF0969DA)
private val DarkOnPrimaryContainer = Color(0xFFECEFFF)
private val DarkInversePrimary = Color(0xFF005BC0)
private val DarkSecondary = Color(0xFFC3C6CF)
private val DarkOnSecondary = Color(0xFF2D3137)
private val DarkSecondaryContainer = Color(0xFF454950)
private val DarkOnSecondaryContainer = Color(0xFFB5B8C1)
private val DarkTertiary = Color(0xFFD4BBFF)
private val DarkOnTertiary = Color(0xFF3F0C84)
private val DarkTertiaryContainer = Color(0xFF7E57C5)
private val DarkOnTertiaryContainer = Color(0xFFF6EDFF)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkBackground = Color(0xFF10131A)
private val DarkOnBackground = Color(0xFFE1E2EC)
private val DarkSurfaceVariant = Color(0xFF32353C)

// Light theme tokens — ligh_themes/reposwipe_light/DESIGN.md
private val LightSurface = Color(0xFFF7F9FF)
private val LightSurfaceDim = Color(0xFFD2DBE6)
private val LightSurfaceBright = Color(0xFFF7F9FF)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFEDF4FF)
private val LightSurfaceContainer = Color(0xFFE6EFFA)
private val LightSurfaceContainerHigh = Color(0xFFE0E9F5)
private val LightSurfaceContainerHighest = Color(0xFFDAE3EF)
private val LightOnSurface = Color(0xFF141C25)
private val LightOnSurfaceVariant = Color(0xFF424753)
private val LightInverseSurface = Color(0xFF29313A)
private val LightInverseOnSurface = Color(0xFFE9F2FD)
private val LightOutline = Color(0xFF727785)
private val LightOutlineVariant = Color(0xFFC2C6D6)
private val LightPrimary = Color(0xFF0051AE)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF0969DA)
private val LightOnPrimaryContainer = Color(0xFFECEFFF)
private val LightInversePrimary = Color(0xFFADC6FF)
private val LightSecondary = Color(0xFF5B5F64)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFDDE0E6)
private val LightOnSecondaryContainer = Color(0xFF5F6369)
private val LightTertiary = Color(0xFF913900)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFB84B00)
private val LightOnTertiaryContainer = Color(0xFFFFECE5)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF93000A)
private val LightBackground = Color(0xFFF7F9FF)
private val LightOnBackground = Color(0xFF141C25)
private val LightSurfaceVariant = Color(0xFFDAE3EF)

val RepoSwipeDarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        inversePrimary = DarkInversePrimary,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceTint = DarkPrimary,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        surfaceBright = DarkSurfaceBright,
        surfaceDim = DarkSurfaceDim,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        surfaceContainerLow = DarkSurfaceContainerLow,
        surfaceContainerLowest = DarkSurfaceContainerLowest,
    )

val RepoSwipeLightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        inversePrimary = LightInversePrimary,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceTint = LightPrimary,
        inverseSurface = LightInverseSurface,
        inverseOnSurface = LightInverseOnSurface,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        surfaceBright = LightSurfaceBright,
        surfaceDim = LightSurfaceDim,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        surfaceContainerLow = LightSurfaceContainerLow,
        surfaceContainerLowest = LightSurfaceContainerLowest,
    )

// Repo card swipe-feedback overlays (not part of the M3 scheme, used by SwipeDeck in Faz 4)
val SwipeRightOverlay = Color(0xFF2ECC71)
val SwipeLeftOverlay = Color(0xFFE74C3C)

// GitHub language-badge dot colors referenced in the mockups
val LanguageTypeScript = Color(0xFF3178C6)
val LanguageJavaScript = Color(0xFFF1E05A)
val LanguagePython = Color(0xFF3572A5)
val LanguageRust = Color(0xFFDEA584)
val LanguageGo = Color(0xFF00ADD8)
val LanguageJava = Color(0xFFB07219)
val LanguageKotlin = Color(0xFFA97BFF)
val LanguageSwift = Color(0xFFF05138)
val LanguageC = Color(0xFF555555)
val LanguageCpp = Color(0xFFF34B7D)
val LanguageCSharp = Color(0xFF178600)
val LanguagePhp = Color(0xFF4F5D95)
val LanguageRuby = Color(0xFF701516)
val LanguageHtml = Color(0xFFE34C26)
val LanguageCss = Color(0xFF563D7C)
val LanguageShell = Color(0xFF89E051)
val LanguageDart = Color(0xFF00B4AB)
val LanguageScala = Color(0xFFC22D40)
val LanguageVue = Color(0xFF41B883)

private val LanguageColorsByName: Map<String, Color> =
    mapOf(
        "TypeScript" to LanguageTypeScript,
        "JavaScript" to LanguageJavaScript,
        "Python" to LanguagePython,
        "Rust" to LanguageRust,
        "Go" to LanguageGo,
        "Java" to LanguageJava,
        "Kotlin" to LanguageKotlin,
        "Swift" to LanguageSwift,
        "C" to LanguageC,
        "C++" to LanguageCpp,
        "C#" to LanguageCSharp,
        "PHP" to LanguagePhp,
        "Ruby" to LanguageRuby,
        "HTML" to LanguageHtml,
        "CSS" to LanguageCss,
        "Shell" to LanguageShell,
        "Dart" to LanguageDart,
        "Scala" to LanguageScala,
        "Vue" to LanguageVue,
    )

/** GitHub's per-language "linguist" color, or null for anything not in the common set. */
fun languageColor(name: String?): Color? = name?.let { LanguageColorsByName[it] }
