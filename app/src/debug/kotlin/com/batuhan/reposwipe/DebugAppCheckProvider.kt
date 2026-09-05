package com.batuhan.reposwipe

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// DebugAppCheckProviderFactory ships in a debugImplementation-only dependency (app/build.gradle.kts),
// so referencing it from src/main directly would fail to link release builds. This debug-source-set
// file is the only place that class gets referenced; src/release/.../DebugAppCheckProvider.kt is its
// release counterpart, returning null instead.
internal fun debugAppCheckProviderFactoryOrNull(): AppCheckProviderFactory? = DebugAppCheckProviderFactory.getInstance()
