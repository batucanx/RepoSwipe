# :baselineprofile

Macrobenchmark module. Two jobs: **generate** the Baseline Profile that ships in the release APK,
and **measure** whether it actually helped.

This module is never shipped — it's a `com.android.test` module that drives `:app` from the outside
with UiAutomator.

## Why a Baseline Profile

Without one, ART interprets app code on first run and only JIT-compiles it after it's been hot for
a while. For a Compose app that means the first launch, and the first frames of every screen, run
at their slowest. A Baseline Profile is a list of the classes and methods the app actually runs on
its important paths; ART AOT-compiles exactly those at install time. Typical effect is roughly
20–30% faster cold start and noticeably fewer dropped frames early on.

## Generating the profile

```bash
./gradlew :app:generateReleaseBaselineProfile
```

Works on a plain retail device (not just a rooted emulator) from **API 33 onward** — ART exposes
shell-level profile dumping to any `profileable`/debuggable build without root from that version,
and the Baseline Profile plugin already marks the benchmark build type `profileable`. Below API 33
you need a rooted emulator or an AOSP/userdebug image instead.

Three real-world gotchas hit generating this against a physical Samsung device (Android 16), worth
checking before assuming something's actually broken:

1. **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`** — the generator installs a *release*-signed APK; if a
   debug-signed build of the app is already on the device, Android refuses to install over it.
   Uninstall the app first.
2. **OEM battery/process management can freeze the app mid-measurement** (Samsung: `FreecessHandler`
   in logcat). Disable the device's global "auto-sleep unused apps" setting — there's no per-app fix
   available since the app isn't installed until the run starts.
3. **Leave the device completely untouched during the run** (~40-60s). Any interaction steals
   foreground focus from the activity the benchmark is trying to confirm launched.

If launch still can't be confirmed (`Unable to confirm activity launch completion`) even though
`adb logcat` shows `ActivityTaskManager: Displayed com.batuhan.reposwipe/.MainActivity`, the
`androidx.benchmark` version is likely too old to parse that Android version's `dumpsys gfxinfo`
output — bump the `benchmark` version in `gradle/libs.versions.toml` and retry before suspecting the
app. (This project hit exactly that on Android 16 with 1.3.4; 1.4.1 fixed it.)

Output lands in `app/src/release/generated/baselineProfiles/` and **should be committed** — that
way the profile ships even though CI has no emulator to generate one.

## Measuring the effect

```bash
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
```

Runs `StartupBenchmark`, which times cold start twice: once with nothing AOT-compiled and once
with the generated profile applied. Compare `timeToInitialDisplayMs` between the two runs — that
gap is what the profile buys. Results also land as JSON under
`baselineprofile/build/outputs/connected_android_test_additional_output/`.

## Coverage limit

RepoSwipe gates everything behind GitHub Device Flow auth, and the generator runs with no
credentials. So the recorded path is process start → auth screen: Hilt's component graph, Sentry
and Coil init in `RepoSwipeApp.onCreate`, `MainActivity`, splash, Compose runtime/theme setup, the
nav host. That is the cold-start path and the highest-value thing to have AOT-compiled, so it's
worth shipping as-is.

It does **not** cover the swipe deck, the detail sheet, or any list screen. Extending it means
giving the generator a signed-in state to start from — a debug-only token seed, or a UiAutomator
pass through the device-flow screen against a test account. `ProfileScenarios` is where that would
slot in, so the generator and the benchmark keep exercising the same journey.

## Why `./gradlew build` doesn't run any of this

`build` is the project's single verification command and runs in CI, where there's no device. This
module's `build` is redefined (see `build.gradle.kts`) to mean "compile the benchmark sources and
run static analysis" — everything else here needs hardware and is invoked deliberately with the
commands above.
