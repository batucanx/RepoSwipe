package com.batuhan.reposwipe.feature.leaderboard

import com.batuhan.reposwipe.core.data.LeaderboardRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.LeaderboardEntry
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val leaderboardRepository = mockk<LeaderboardRepository>()
    private val starRepository = mockk<StarRepository>()

    private val entry =
        LeaderboardEntry(
            repoId = 1L,
            repoName = "reposwipe",
            ownerLogin = "batucanx",
            description = "A swipe-to-star GitHub client",
            language = "Kotlin",
            htmlUrl = "https://github.com/batucanx/reposwipe",
            swipeCount = 5L,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { starRepository.observePendingStarStates() } returns flowOf(emptyMap())
    }

    private fun viewModel() = LeaderboardViewModel(leaderboardRepository, starRepository)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates entries on success`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns listOf(entry)

            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(listOf(entry), state.entries)
        }

    @Test
    fun `initial load surfaces an error and keeps entries empty on failure`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } throws RuntimeException("offline")

            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.entries.isEmpty())
        }

    @Test
    fun `refresh re-fetches page one and clears a previous error`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns emptyList()
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns listOf(entry)
            viewModel.refresh()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals(listOf(entry), state.entries)
            coVerify(exactly = 2) { leaderboardRepository.getLeaderboardPage(reset = true) }
        }

    @Test
    fun `loadMore appends to the existing entries`() =
        runTest {
            // A full page (matching PAGE_SIZE) is required so hasMore is true and loadMore
            // doesn't short-circuit.
            val firstPage = (1..LeaderboardRepository.PAGE_SIZE).map { entry.copy(repoId = it.toLong()) }
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns firstPage
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            val secondEntry = entry.copy(repoId = 999L, repoName = "other-repo")
            coEvery { leaderboardRepository.getLeaderboardPage(reset = false) } returns listOf(secondEntry)
            viewModel.loadMore()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(firstPage + secondEntry, viewModel.uiState.value.entries)
        }

    @Test
    fun `toggleStar on an unstarred entry stars it on GitHub`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns emptyList()
            coEvery { starRepository.starRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.starredStates.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleStar(entry)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.starRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }

    @Test
    fun `toggleStar on an already-starred entry unstars it`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns
                flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { starRepository.unstarRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.starredStates.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleStar(entry)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.unstarRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }

    @Test
    fun `starredStates reflects the star repository's pending states`() =
        runTest {
            coEvery { leaderboardRepository.getLeaderboardPage(reset = true) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns
                flowOf(mapOf("batucanx/reposwipe" to true))
            val viewModel = viewModel()
            val collectJob = launch { viewModel.starredStates.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(mapOf("batucanx/reposwipe" to true), viewModel.starredStates.value)
            collectJob.cancel()
        }
}
