package com.batuhan.reposwipe.feature.settings

import com.batuhan.reposwipe.core.common.theme.ThemeMode
import com.batuhan.reposwipe.core.datastore.ThemePreferencesDataStore
import com.batuhan.reposwipe.core.datastore.TokenDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val themePreferencesDataStore = mockk<ThemePreferencesDataStore>()
    private val tokenDataStore = mockk<TokenDataStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects the stored theme mode`() =
        runTest {
            every { themePreferencesDataStore.themeMode } returns flowOf(ThemeMode.DARK)

            val viewModel = SettingsViewModel(themePreferencesDataStore, tokenDataStore)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        }

    @Test
    fun `setThemeMode delegates to the preferences data store`() =
        runTest {
            every { themePreferencesDataStore.themeMode } returns flowOf(ThemeMode.SYSTEM)
            coEvery { themePreferencesDataStore.setThemeMode(any()) } returns Unit
            val viewModel = SettingsViewModel(themePreferencesDataStore, tokenDataStore)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.setThemeMode(ThemeMode.LIGHT)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { themePreferencesDataStore.setThemeMode(ThemeMode.LIGHT) }
        }

    @Test
    fun `signOut clears the stored access token`() =
        runTest {
            every { themePreferencesDataStore.themeMode } returns flowOf(ThemeMode.SYSTEM)
            coEvery { tokenDataStore.clearAccessToken() } returns Unit
            val viewModel = SettingsViewModel(themePreferencesDataStore, tokenDataStore)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.signOut()
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { tokenDataStore.clearAccessToken() }
        }
}
