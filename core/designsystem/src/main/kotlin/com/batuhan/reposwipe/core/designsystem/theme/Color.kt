package com.batuhan.reposwipe.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark theme tokens — "Matte Neutral + Violet" reskin (2026-08-10, replacing "GitHub Dark
// Modern" per user feedback that Primer's canvas tones read too blue-gray to feel truly "matte
// black", and that the swipe-right/like green had gone stale/generic). The neutral ramp below
// (background/surface containers/on-surface/outline) is now desaturated near-true-gray rather
// than Primer's blue-tinted canvas.*/fg.* family. Primary/accent moved from Primer's
// btn.primary.bg (success green) to Primer's own "done"/Copilot purple (btn.done.emphasis ≈
// #8957E5) — chosen over the alternatives (Linear-style indigo, flame orange) because it also
// extends LeaderboardScreen's pre-existing blue-purple #1 gradient into the app's main accent
// instead of introducing an unrelated hue. Verified to keep ~4.6:1 contrast against white
// (button fill role) and near-identical luminance to the old green against dark surfaces
// (foreground/icon-tint role), so no accessibility regression from the swap. Error still traces
// to Primer's btn.danger.bg (untouched — only the like/right side of the swipe pair changed).
//
// **Update (2026-09-01):** background/surface/surfaceDim pushed from near-black (#0A0A0B) to
// true #000000 per explicit user request for a more modern true-black look — the earlier
// off-black choice traded that look away specifically to avoid OLED smearing on scroll, so
// revisit that tradeoff if scrolling smear complaints show up on OLED devices. surfaceContainer
// and up are untouched (still the lighter charcoal ramp) so raised cards keep reading as raised
// against the now-pure-black canvas.
private val DarkSurface = Color(0xFF000000)
private val DarkSurfaceDim = Color(0xFF000000)
private val DarkSurfaceBright = Color(0xFF3A3A3D)
private val DarkSurfaceContainerLowest = Color(0xFF000000)
private val DarkSurfaceContainerLow = Color(0xFF121214)

// Darkened again (2026-09-03, explicit user request) from #18181B to sit right next to true
// black — card boundary still reads fine since the card also carries its own 1dp onSurface-alpha
// border, so it doesn't depend on fill-color contrast against the page background alone.
private val DarkSurfaceContainer = Color(0xFF0E0E10) // repo card background
private val DarkSurfaceContainerHigh = Color(0xFF26262A)
private val DarkSurfaceContainerHighest = Color(0xFF323235)
private val DarkOnSurface = Color(0xFFF2F2F4)
private val DarkOnSurfaceVariant = Color(0xFF9C9CA3)
private val DarkInverseSurface = Color(0xFFF2F2F4)
private val DarkInverseOnSurface = Color(0xFF0A0A0B)
private val DarkOutline = Color(0xFF6E6E76)
private val DarkOutlineVariant = Color(0xFF323235)
private val DarkPrimary = Color(0xFFF2F2F4) // general text/icon color
private val DarkOnPrimary = Color(0xFF0A0A0B)

// French-tricolor reskin exploration (2026-09-04, user request): the purple accent ("Matte
// Neutral + Violet") didn't land, so the app's general accent role — quick-view button, filter
// chips, selected states, brand shimmer/glow (see BrandAccent below), everywhere else that isn't
// the reject/like/nav-active red — moves to this GitHub-blue, matching the blue LeaderboardScreen
// already used as the #1 gradient's start stop rather than introducing an unrelated new hue. The
// like/right swipe action itself moved to red instead (see SwipeScreen.kt's Like button + the
// SwipeRightOverlay override below) rather than following this accent.
private val DarkPrimaryContainer = Color(0xFF0969DA)
private val DarkOnPrimaryContainer = Color(0xFFFFFFFF)
private val DarkInversePrimary = Color(0xFF0969DA)
private val DarkSecondary = Color(0xFF9C9CA3) // muted text/icons
private val DarkOnSecondary = Color(0xFF0A0A0B)
private val DarkSecondaryContainer = Color(0xFF26262A)
private val DarkOnSecondaryContainer = Color(0xFFF2F2F4)
private val DarkTertiary = Color(0xFFF2F2F4)
private val DarkOnTertiary = Color(0xFF0A0A0B)
private val DarkTertiaryContainer = Color(0xFF26262A)
private val DarkOnTertiaryContainer = Color(0xFFF2F2F4)
private val DarkError = Color(0xFFDA3633) // swipe-left/pass — Primer btn.danger.bg
private val DarkOnError = Color(0xFFFFFFFF)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkBackground = Color(0xFF000000)
private val DarkOnBackground = Color(0xFFF2F2F4)
private val DarkSurfaceVariant = Color(0xFF26262A)

// Brand accent referenced directly by glass components (floating logo badge, primary CTA glow) —
// kept in sync with DarkPrimaryContainer above (GitHub-blue, replacing the "Matte Neutral +
// Violet" purple, which itself replaced "GitHub Dark Modern"'s success green).
val BrandAccent = Color(0xFF0969DA)

// Deliberate one-off accent for the "View on GitHub" CTA in y_ld_zlananlar_reposwipe/code.html —
// a GitHub-blue outbound-link color kept distinct from the lime in-app action color.
val GitHubBlue = Color(0xFF0A66C2)

// French-tricolor reskin exploration (2026-09-04, user request, from the Stitch "Rendez-vous"
// reference's own navy pair #051937/#0B2545): a deep "Parisian night" navy for the swipe card's
// own background specifically — not the shared surfaceContainer token, which stays untouched
// everywhere else it's used (detail-sheet stat tiles, README panel, similar-repo cards, etc.).
// Kept identical in both themes per explicit ask, which is why RepoCard's own text/chip colors
// are hardcoded to fixed light tones rather than the usual theme-driven primary/secondary
// tokens — those tokens flip dark-on-light in light mode (by design, for every *other* light
// surface in the app), which would go illegible against a card background that no longer flips.
val CardBackgroundNavy = Color(0xFF0B2545)

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

// Light theme tokens (Faz 14.2, 2026-08-07) — the design source of truth's `ligh_themes\` folder
// isn't present on this dev machine, so these were originally derived algorithmically from the
// dark palette above rather than hand-picked: the cool blue-gray neutral family (surfaces) and
// the warm olive "neutral-variant" family (onSurfaceVariant/outline) each kept the *original*
// Lumina dark scheme's hue/saturation, with lightness flipped to standard M3 light-mode tone
// targets (surface ~98, onSurface ~10, outline ~47, outlineVariant ~82, etc). Error reuses
// Material 3's own baseline light error tones. **Note:** the dark scheme was later reskinned to
// "GitHub Dark Modern" (see the dark tokens above) and its own neutral-variant family dropped
// the olive tint in favor of true grays — this light scheme predates that change and was
// intentionally left alone (the dark-theme update request didn't ask for light-mode changes), so
// its neutral-variant olive tint no longer traces back to a literal shared source in the dark
// scheme the way it originally did. Still a fine, legible light palette on its own.
//
// Primary/secondary/tertiary needed a role remap rather than a plain lightness-flip: this design
// deliberately sets dark-mode's primary/tertiary to pure white (readable text/icon color on a
// near-black surface), which is illegible as a light-mode "primary" on a near-white surface.
//
// The first pass (2026-08-07) tried keeping green as light mode's brand color — primary =
// #3E7100 (a saturated green), primaryContainer = the old Lumina lime as-is — but real-device
// testing showed both read poorly on light backgrounds: `primaryContainer` in particular is used
// all over the app (RepoListItem.kt/RepoCard.kt/ProfileScreen.kt icon tints and stat numbers,
// RepoSwipeBottomNavBar.kt's selected pill, SwipeScreen.kt's "like" action button) as a
// *foreground* content color, not just a container fill, and a bright lime foreground on a
// near-white background is close to unreadable. Per that feedback, light mode now drops green
// entirely from primary/primaryContainer and uses near-black instead (matching LightOnSurface),
// so every one of those texts/icons/buttons renders as black-on-light — clearly legible — while
// still functioning as the "accent" role. onPrimaryContainer flips from dark-green-on-lime to
// white-on-near-black to stay readable against its now-dark container (e.g. the bottom nav's
// selected pill). Dark mode is untouched; this only affects light-mode legibility.
// - secondary/tertiary are near-achromatic in the dark scheme (a plain neutral gray, not a real
//   hue), so they get a standard M3 tone inversion (dark's ~80/~20/~30/~90 tones become light's
//   ~40/~100/~90/~15-20), with tertiary reusing the same neutral so it doesn't fight for
//   attention against the app's one real accent color (primary).
private val LightSurface = Color(0xFFF9FAFB)
private val LightSurfaceDim = Color(0xFFDADEE2)
private val LightSurfaceBright = Color(0xFFF9FAFB)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF4F5F6)
private val LightSurfaceContainer = Color(0xFFEEF0F2)
private val LightSurfaceContainerHigh = Color(0xFFE8EBED)
private val LightSurfaceContainerHighest = Color(0xFFE2E6E9)
private val LightOnSurface = Color(0xFF161A1D)
private val LightOnSurfaceVariant = Color(0xFF4E5940)
private val LightInverseSurface = Color(0xFF2D3339)
private val LightInverseOnSurface = Color(0xFFF4F5F6)
private val LightOutline = Color(0xFF7A8B65)
private val LightOutlineVariant = Color(0xFFD2D8CA)
private val LightSurfaceVariant = Color(0xFFE2E6E9)
private val LightBackground = Color(0xFFF9FAFB)
private val LightOnBackground = Color(0xFF161A1D)
private val LightPrimary = LightOnSurface
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = LightOnSurface
private val LightOnPrimaryContainer = Color(0xFFFFFFFF)
private val LightInversePrimary = Color(0xFF3A6A00)
private val LightSecondary = Color(0xFF636669)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFE5E6E6)
private val LightOnSecondaryContainer = Color(0xFF252627)
private val LightTertiary = Color(0xFF3E4041)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFE5E6E6)
private val LightOnTertiaryContainer = Color(0xFF323334)

// Material 3's own baseline light error scheme — pairs with the dark error tones above, which
// are M3's baseline dark error scheme verbatim.
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)

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

// Repo card swipe-feedback overlays — kept in sync with the Like/Pass button colors so the drag
// feedback visually matches the buttons it previews the outcome of. Both are red now (2026-09-04,
// user request) since the Like button itself moved off the blue brand accent to a filled-red
// heart — see SwipeScreen.kt's Like [SwipeActionButton]. Same literal red in both cases,
// independent of [DarkError]/light theme's error tone, matching how this pair worked before.
val SwipeRightOverlay = Color(0xFFDA3633)
val SwipeLeftOverlay = Color(0xFFDA3633)

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
