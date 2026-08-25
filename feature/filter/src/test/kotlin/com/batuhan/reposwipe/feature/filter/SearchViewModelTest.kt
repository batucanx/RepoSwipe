package com.batuhan.reposwipe.feature.filter

import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.Repo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repoRepository = mockk<RepoRepository>()
    private val starRepository = mockk<StarRepository>()

    private val testRepo =
        Repo(
            id = 1L,
            name = "reposwipe",
            ownerLogin = "batucanx",
            ownerAvatarUrl = null,
            description = "A swipe-to-star GitHub client",
            starCount = 42,
            forkCount = 3,
            language = "Kotlin",
            updatedAt = "2026-01-01T00:00:00Z",
            htmlUrl = "https://github.com/batucanx/reposwipe",
            headerImageUrl = "",
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { starRepository.observePendingStarStates() } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SearchViewModel(repoRepository, starRepository)

    @Test
    fun `no search has been submitted yet on a fresh viewModel`() {
        val viewModel = viewModel()

        assertNull(viewModel.submittedQuery.value)
    }

    @Test
    fun `onQueryChange updates the draft query without submitting`() {
        val viewModel = viewModel()

        viewModel.onQueryChange("react")

        assertEquals("react", viewModel.query.value)
        assertNull(viewModel.submittedQuery.value)
    }

    @Test
    fun `submit trims whitespace and sets the submitted query`() {
        val viewModel = viewModel()
        viewModel.onQueryChange("  react  ")

        viewModel.submit()

        assertEquals("react", viewModel.submittedQuery.value)
    }

    @Test
    fun `submitting a blank query is a no-op`() {
        val viewModel = viewModel()
        viewModel.onQueryChange("   ")

        viewModel.submit()

        assertNull(viewModel.submittedQuery.value)
    }

    @Test
    fun `toggleStar on an unstarred repo stars it on GitHub`() =
        runTest {
            coEvery { starRepository.starRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.starredStates.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleStar(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.starRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }

    @Test
    fun `toggleStar on an already-starred repo unstars it`() =
        runTest {
            every { starRepository.observePendingStarStates() } returns
                flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { starRepository.unstarRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.starredStates.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleStar(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.unstarRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }
}
