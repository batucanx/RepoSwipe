package com.batuhan.reposwipe.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which classes and methods RepoSwipe actually runs on the paths exercised below, and
 * writes them out as a Baseline Profile that ships inside the APK. ART then AOT-compiles exactly
 * those methods at install time instead of interpreting them and JIT-ing later, which is what
 * removes the first-run stutter on cold start and the first frames of a screen.
 *
 * Run it with:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * on a **rooted emulator or an AOSP/userdebug system image, API 28+** — ART refuses to hand a
 * profile back on a locked-down production device, so this cannot run on an ordinary retail
 * phone. The output lands in `app/src/release/generated/baselineProfiles/` and is meant to be
 * committed, so the profile ships even when CI has no emulator.
 *
 * ## Coverage limit worth knowing about
 *
 * RepoSwipe gates everything behind GitHub Device Flow auth, and this generator drives the app
 * with no credentials — so what it can reach is process start through to the auth screen: Hilt's
 * component graph, Sentry + Coil init in `RepoSwipeApp.onCreate`, `MainActivity`, the splash
 * screen, Compose runtime/theme setup and the nav host. That *is* the cold-start path and the
 * single most valuable thing to have AOT-compiled, so this is worth shipping as-is.
 *
 * It does **not** cover the swipe deck, the detail sheet, or any list screen, because it can't
 * log in. Extending it means giving the generator a signed-in state to start from (a debug-only
 * token seed, or a UiAutomator pass through the device-flow screen against a test account) — see
 * [ProfileScenarios] for where that would slot in.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            // Also emits a *startup* profile, a smaller subset ART prioritises specifically for
            // the launch path — worth having on top of the full profile.
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            ProfileScenarios.awaitFirstScreen(device)
        }
    }
}

internal const val TARGET_PACKAGE = "com.batuhan.reposwipe"
