plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.batuhan.reposwipe.baselineprofile"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        // 28 is the floor for Baseline Profile collection (ART needs to be able to dump profiles);
        // the app itself still ships minSdk 26 and simply gets no profile on 26/27.
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Macrobenchmarks measure a real, R8-processed build — a debug build's numbers are meaningless
    // (no minification, debuggable overhead, interpreted paths everywhere). The Baseline Profile
    // plugin creates and wires the `nonMinifiedRelease`/`benchmarkRelease` variants on both this
    // module and :app itself, so neither declares them by hand.
    targetProjectPath = ":app"
}

// Generation needs a rooted emulator or an AOSP/userdebug system image — ART will not hand over a
// profile on a locked-down production device. See this module's README for the exact command.
baselineProfile {
    useConnectedDevices = true
}

// `./gradlew build` is this project's single verification command (see CLAUDE.md) and is what CI
// runs, where no device or emulator exists. This module's `assemble` transitively pulls in
// `collectNonMinifiedReleaseBaselineProfile`, which runs the generator on a device — so leaving
// `build` at its default meaning kills the whole build with "No connected devices!", turning a
// green-on-every-machine command into one that only works with an emulator attached. (The link is
// an implicit input dependency on the connected test's output, not a `dependsOn` edge, so it
// can't be filtered off.)
//
// So `build` here means "compile the benchmark sources and run static analysis" — which is all CI
// can meaningfully verify about a module whose entire output requires hardware. The compile tasks
// are named explicitly rather than left to `assemble` so a broken benchmark still fails the build
// instead of going unnoticed. Producing profiles and running benchmarks stays a deliberate,
// device-attached act:
//   ./gradlew :app:generateReleaseBaselineProfile
//   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
tasks.named("build") {
    setDependsOn(listOf("check", "compileNonMinifiedReleaseSources", "compileBenchmarkReleaseSources"))
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
