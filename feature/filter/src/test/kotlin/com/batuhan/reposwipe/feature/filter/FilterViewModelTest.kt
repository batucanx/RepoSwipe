package com.batuhan.reposwipe.feature.filter

import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val discoverFilterRepository = mockk<DiscoverFilterRepository>()
    private val filters = MutableStateFlow(DiscoverFilters())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { discoverFilterRepository.filters } returns filters
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FilterViewModel(discoverFilterRepository)

    @Test
    fun `uiState reflects the repository's current filters`() =
        runTest(dispatcher) {
            filters.value = DiscoverFilters(languages = setOf("Kotlin"), minStars = 100)

            val viewModel = viewModel()
            // uiState is a WhileSubscribed StateFlow — it only starts collecting the repository's
            // filters once something subscribes to it, so a real collector is needed here.
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(DiscoverFilters(languages = setOf("Kotlin"), minStars = 100), viewModel.uiState.value.filters)
            collectJob.cancel()
        }

    @Test
    fun `toggleLanguage, toggleTopic, setMinStars, setUpdatedRecently and reset all delegate`() {
        every { discoverFilterRepository.toggleLanguage(any()) } returns Unit
        every { discoverFilterRepository.toggleTopic(any()) } returns Unit
        every { discoverFilterRepository.setMinStars(any()) } returns Unit
        every { discoverFilterRepository.setUpdatedRecently(any()) } returns Unit
        every { discoverFilterRepository.reset() } returns Unit
        val viewModel = viewModel()

        viewModel.toggleLanguage("Kotlin")
        viewModel.toggleTopic("mobile")
        viewModel.setMinStars(500)
        viewModel.setUpdatedRecently(true)
        viewModel.reset()

        verify { discoverFilterRepository.toggleLanguage("Kotlin") }
        verify { discoverFilterRepository.toggleTopic("mobile") }
        verify { discoverFilterRepository.setMinStars(500) }
        verify { discoverFilterRepository.setUpdatedRecently(true) }
        verify { discoverFilterRepository.reset() }
    }
}
