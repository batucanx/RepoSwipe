package com.batuhan.reposwipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import com.batuhan.reposwipe.feature.auth.navigation.AUTH_ROUTE
import com.batuhan.reposwipe.feature.swipe.navigation.SWIPE_ROUTE
import com.batuhan.reposwipe.navigation.RepoSwipeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash stays up (see Theme.RepoSwipe.Splash in the manifest) until the stored
        // GitHub token has been read, so we never flash the auth screen at cold start just
        // because the check hadn't finished yet.
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.uiState.value is MainActivityUiState.Loading
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme =
                when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            RepoSwipeTheme(darkTheme = darkTheme) {
                when (val state = viewModel.uiState.collectAsStateWithLifecycle().value) {
                    MainActivityUiState.Loading -> Unit
                    is MainActivityUiState.Success -> {
                        // NavHost reads startDestination only once when its graph is built,
                        // so freeze it here — later isAuthenticated flips are handled
                        // reactively inside RepoSwipeNavHost instead of rebuilding the graph.
                        val startDestination = remember { if (state.isAuthenticated) SWIPE_ROUTE else AUTH_ROUTE }
                        RepoSwipeNavHost(
                            startDestination = startDestination,
                            isAuthenticated = state.isAuthenticated,
                        )
                    }
                }
            }
        }
    }
}
