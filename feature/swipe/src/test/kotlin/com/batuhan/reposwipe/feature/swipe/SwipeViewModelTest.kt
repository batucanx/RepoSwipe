package com.batuhan.reposwipe.feature.swipe

import androidx.paging.PagingData
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.DiscoverFilters
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.network.RateLimitObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val repoRepository = mockk<RepoRepository>()
    private val starRepository = mockk<StarRepository>()
    private val leaderboardRepository = mockk<LeaderboardRepository>()
    private val discoverFilterRepository = mockk<DiscoverFilterRepository>()

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
        every { repoRepository.searchRepos(any()) } returns flowOf(PagingData.empty())
        every { discoverFilterRepository.filters } returns MutableStateFlow(DiscoverFilters())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SwipeViewModel(
            repoRepository = repoRepository,
            starRepository = starRepository,
            leaderboardRepository = leaderboardRepository,
            discoverFilterRepository = discoverFilterRepository,
            rateLimitObserver = RateLimitObserver(),
        )

    @Test
    fun `swiping right stars the repo and records the swipe`() =
        runTest {
            coEvery { starRepository.starRepo(any(), any()) } returns Unit
            coEvery { leaderboardRepository.recordSwipe(any()) } returns Unit
            val viewModel = viewModel()

            viewModel.onSwiped(testRepo, SwipeDirection.Right)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.starRepo("batucanx", "reposwipe") }
            coVerify { leaderboardRepository.recordSwipe(testRepo) }
        }

    @Test
    fun `swiping left does not star or record anything`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onSwiped(testRepo, SwipeDirection.Left)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { starRepository.starRepo(any(), any()) }
            coVerify(exactly = 0) { leaderboardRepository.recordSwipe(any()) }
        }

    @Test
    fun `a failed star request does not crash the swipe`() =
        runTest {
            coEvery { starRepository.starRepo(any(), any()) } throws RuntimeException("network down")
            coEvery { leaderboardRepository.recordSwipe(any()) } returns Unit
            val viewModel = viewModel()

            viewModel.onSwiped(testRepo, SwipeDirection.Right)
            dispatcher.scheduler.advanceUntilIdle()

            // Reaching this line means the failure inside onSwiped's launch block was swallowed
            // (via runCatching), not propagated as an uncaught coroutine exception.
            coVerify { leaderboardRepository.recordSwipe(testRepo) }
        }

    @Test
    fun `swiping advances the index, rewind steps it back without going negative`() =
        runTest {
            coEvery { starRepository.starRepo(any(), any()) } returns Unit
            coEvery { leaderboardRepository.recordSwipe(any()) } returns Unit
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onSwiped(testRepo, SwipeDirection.Left)
            assertEquals(1, viewModel.currentIndex.value)

            viewModel.onRewind()
            assertEquals(0, viewModel.currentIndex.value)

            viewModel.onRewind()
            assertEquals(0, viewModel.currentIndex.value)
        }

    @Test
    fun `toggleLanguage delegates to the filter repository`() {
        every { discoverFilterRepository.toggleLanguage(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.toggleLanguage("Rust")

        verify { discoverFilterRepository.toggleLanguage("Rust") }
    }
}
