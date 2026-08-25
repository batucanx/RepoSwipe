package com.batuhan.reposwipe.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold start twice — once with no Baseline Profile and once with the generated one — so
 * the profile's effect is an actual number rather than an assumption. Run with:
 * ```
 * ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * ```
 * Results print to the test output and land in a JSON report under
 * `baselineprofile/build/outputs/connected_android_test_additional_output/`.
 *
 * Compare `timeToInitialDisplayMs` between the two: [startupNoCompilation] is the floor (nothing
 * AOT-compiled) and [startupWithBaselineProfile] is what a user installing from Play actually
 * gets. If the gap is small the profile isn't covering the right paths — most likely because it
 * only reaches the auth screen, see [BaselineProfileGenerator]'s doc.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = measureStartup(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() = measureStartup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

    private fun measureStartup(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            // Cold: the process is killed between runs, so this measures the real first-launch
            // cost — class loading, Hilt graph construction, Compose runtime warm-up — which is
            // exactly what a Baseline Profile changes. A warm/hot measurement would mostly show
            // the profile making no difference, because the work has already happened.
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
        ) {
            pressHome()
            startActivityAndWait()
            ProfileScenarios.awaitFirstScreen(device)
        }

    private companion object {
        // Enough samples for the harness to report a stable median without making a full run
        // take longer than a coffee break.
        const val ITERATIONS = 10
    }
}
