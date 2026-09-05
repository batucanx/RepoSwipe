import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.sentry)
}

// Sentry config — never committed. Add these lines to the (gitignored) root local.properties:
//   sentry.dsn=https://...        (blank DSN makes the Sentry SDK itself a no-op)
//   sentry.org=...
//   sentry.project=...
//   sentry.authToken=...          (blank token skips ProGuard mapping upload instead of failing the build)
val sentryProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
val sentryDsn: String = sentryProperties.getProperty("sentry.dsn", "")
val sentryAuthToken: String = sentryProperties.getProperty("sentry.authToken", "")

// Release signing — keystore.properties (gitignored) and the .keystore file it points to are
// never committed. Without it, `assembleRelease`/`bundleRelease` fall back to no signing config
// at all (an unsigned, uninstallable release build) rather than silently signing with debug keys.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

android {
    namespace = "com.batuhan.reposwipe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.batuhan.reposwipe"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // local.properties' sdk.dir is written unescaped (C:/...) by Android Studio itself
        // on every sync on Windows; hand-escaping it just gets overwritten again.
        disable += "PropertyEscape"
    }
}

// Uploads the release build's ProGuard/R8 mapping file to Sentry so crash stack traces from
// production (isMinifyEnabled = true, above) show real symbol names instead of obfuscated ones.
// Auto-upload is skipped entirely — rather than failing the build — when no auth token is
// configured, same no-op-when-unconfigured shape as the SENTRY_DSN handling above.
sentry {
    org.set(sentryProperties.getProperty("sentry.org", ""))
    projectName.set(sentryProperties.getProperty("sentry.project", ""))
    authToken.set(sentryAuthToken)
    autoUploadProguardMapping.set(sentryAuthToken.isNotBlank())
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:swipe"))
    implementation(project(":feature:filter"))
    implementation(project(":feature:leaderboard"))
    implementation(project(":feature:starred"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:followers"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.hilt.compiler)

    // Registers a GIF/SVG-capable ImageLoader as the app-wide Coil default (RepoSwipeApp) —
    // README previews commonly embed an animated .gif demo or SVG badges, neither of which Coil
    // can decode without these.
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    implementation(libs.sentry.android)

    // App Check: attests that Firestore/Auth calls come from a genuine, unmodified build of this
    // app before Firebase honors them. Debug provider lets local/debug builds pass without a real
    // Play Integrity attestation — see RepoSwipeApp.onCreate() for which provider gets installed.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Installs the Baseline Profile that :baselineprofile generates (and the ones Compose/Paging
    // ship inside their own AARs) into ART at first run. Without this dependency the profile is
    // packaged into the APK but never actually applied on API 28-30, where the platform has no
    // built-in installer for it.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
