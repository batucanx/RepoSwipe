package com.batuhan.reposwipe.baselineprofile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * The UI journeys shared between [BaselineProfileGenerator] (which records them) and
 * [StartupBenchmark] (which times them), so the profile is always generated from the same path
 * the measurement claims to represent — if those two drift apart, the benchmark stops being
 * evidence about the profile.
 */
internal object ProfileScenarios {
    /**
     * Waits for the first real frame after the splash screen hands over. `startActivityAndWait`
     * only returns once the activity reports drawn, which for a Compose app can be the splash
     * still holding the window — waiting on the window-update signal instead lets the auth
     * screen's own composition and layout run, so that work lands in the profile rather than
     * being cut off mid-startup.
     */
    fun awaitFirstScreen(device: UiDevice) {
        device.waitForIdle()
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), FIRST_SCREEN_TIMEOUT_MS)
        device.waitForIdle()
    }

    private const val FIRST_SCREEN_TIMEOUT_MS = 5_000L
}
