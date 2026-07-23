package com.batuhan.reposwipe

import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.datastore.ThemePreferencesDataStore
import com.batuhan.reposwipe.core.network.NetworkMonitor
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>()
    private val themePreferencesDataStore = mockk<ThemePreferencesDataStore>()
    private val networkMonitor = mockk<NetworkMonitor>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects authentication, theme and connectivity state`() =
        runTest {
            val isAuthenticated = MutableStateFlow(false)
            every { authRepository.isAuthenticated } returns isAuthenticated
            every { themePreferencesDataStore.themeMode } returns flowOf(ThemeMode.DARK)
            every { networkMonitor.isOnline } returns flowOf(false)

            val viewModel = MainActivityViewModel(authRepository, themePreferencesDataStore, networkMonitor)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(MainActivityUiState.Success(isAuthenticated = false), viewModel.uiState.value)
            assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
            assertTrue(!viewModel.isOnline.value)

            isAuthenticated.value = true
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(MainActivityUiState.Success(isAuthenticated = true), viewModel.uiState.value)
        }
}
