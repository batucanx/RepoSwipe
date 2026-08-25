package com.batuhan.reposwipe.feature.auth.onboarding

import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.datastore.OnboardingPreferencesDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val discoverFilterRepository = mockk<DiscoverFilterRepository>(relaxed = true)
    private val onboardingPreferencesDataStore = mockk<OnboardingPreferencesDataStore>()
    private val hasCompletedOnboarding = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { onboardingPreferencesDataStore.hasCompletedOnboarding } returns hasCompletedOnboarding
        coEvery { onboardingPreferencesDataStore.setOnboardingCompleted() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OnboardingViewModel(discoverFilterRepository, onboardingPreferencesDataStore)

    @Test
    fun `starts loading then resolves to the first step when onboarding was not completed before`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            assertTrue(viewModel.uiState.value.isLoading)

            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.finished)
            assertEquals(OnboardingStep.LANGUAGE, state.step)
        }

    @Test
    fun `skips straight to finished when onboarding was already completed`() =
        runTest(dispatcher) {
            hasCompletedOnboarding.value = true

            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.finished)
        }

    @Test
    fun `selecting a language records it and advances to the topic step`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.selectLanguage("Kotlin")

            val state = viewModel.uiState.value
            assertEquals("Kotlin", state.selectedLanguage)
            assertEquals(OnboardingStep.TOPIC, state.step)
        }

    @Test
    fun `skipping the language step advances without recording a language`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.skipLanguage()

            val state = viewModel.uiState.value
            assertEquals(null, state.selectedLanguage)
            assertEquals(OnboardingStep.TOPIC, state.step)
        }

    @Test
    fun `selecting a topic records it and advances to the popularity step`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.selectTopic("mobile")

            val state = viewModel.uiState.value
            assertEquals("mobile", state.selectedTopic)
            assertEquals(OnboardingStep.POPULARITY, state.step)
        }

    @Test
    fun `finish applies the selected language and topic, seeds min stars when popular-only, and marks onboarding complete`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.selectLanguage("Kotlin")
            viewModel.selectTopic("mobile")

            viewModel.finish(popularOnly = true)
            dispatcher.scheduler.advanceUntilIdle()

            verify { discoverFilterRepository.selectLanguage("Kotlin") }
            verify { discoverFilterRepository.selectTopic("mobile") }
            verify { discoverFilterRepository.setMinStars(1000) }
            coVerify { onboardingPreferencesDataStore.setOnboardingCompleted() }
            assertTrue(viewModel.uiState.value.finished)
        }

    @Test
    fun `finish with nothing selected and popularOnly false still completes onboarding without touching filters`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.finish(popularOnly = false)
            dispatcher.scheduler.advanceUntilIdle()

            verify(exactly = 0) { discoverFilterRepository.selectLanguage(any()) }
            verify(exactly = 0) { discoverFilterRepository.selectTopic(any()) }
            verify(exactly = 0) { discoverFilterRepository.setMinStars(any()) }
            coVerify { onboardingPreferencesDataStore.setOnboardingCompleted() }
            assertTrue(viewModel.uiState.value.finished)
        }
}
