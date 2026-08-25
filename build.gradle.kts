import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // AGP ships application/library/test in one artifact, so `com.android.test` lands on the
    // classpath alongside the two above with no version of its own. Declaring it here (rather
    // than versioned in :baselineprofile) is what lets that module resolve it.
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

// Static analysis (detekt: code smells/complexity, ktlint: formatting) applied uniformly across
// every module — a cross-cutting concern, unlike feature dependencies which genuinely differ
// per module and stay declared in each module's own build.gradle.kts.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        parallel = true
    }

    extensions.configure<KtlintExtension> {
        android.set(true)
    }

    // Applied here rather than in each Compose module's own build file for the same reason detekt
    // and ktlint are: the stability contract is one shared, cross-cutting fact about the codebase,
    // and a module that silently missed it would just quietly lose recomposition skipping with
    // nothing failing to point at it. `plugins.withId` so this only touches modules that actually
    // apply the Compose plugin — :core:data, :core:network etc. have no Compose extension to
    // configure. See the config file itself for what the listed types assert and why.
    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            // Singular `stabilityConfigurationFile`, not the plural `stabilityConfigurationFiles`:
            // the latter only exists from Kotlin 2.1 onward and this project is on 2.0.21.
            stabilityConfigurationFile.set(
                rootProject.layout.projectDirectory.file("config/compose/stability_config.conf"),
            )
        }
    }
}
