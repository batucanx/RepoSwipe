package com.batuhan.reposwipe

import com.google.firebase.appcheck.AppCheckProviderFactory

// Release counterpart of src/debug/.../DebugAppCheckProvider.kt — see that file for why this split
// exists. Always null here; RepoSwipeApp.onCreate() falls back to PlayIntegrityAppCheckProviderFactory.
internal fun debugAppCheckProviderFactoryOrNull(): AppCheckProviderFactory? = null
