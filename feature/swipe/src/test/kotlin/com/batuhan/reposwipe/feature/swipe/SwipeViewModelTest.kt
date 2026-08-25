package com.batuhan.reposwipe.feature.swipe

import androidx.paging.PagingData
import com.batuhan.reposwipe.core.common.model.SwipeDirection
import com.batuhan.reposwipe.core.data.DiscoverFilterRepository
import com.batuhan.reposwipe.core.data.LeaderboardRepository
import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.model.Contributor
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
        every { repoRepository.searchRepos(any<DiscoverFilters>()) } returns flowOf(PagingData.empty())
        every { discoverFilterRepository.filters } returns MutableStateFlow(DiscoverFilters())
        every { starRepository.observePendingStarStates() } returns flowOf(emptyMap())
        coEvery { repoRepository.getLanguageBreakdown(any(), any()) } returns Result.success(emptyMap())
        coEvery { repoRepository.getContributors(any(), any()) } returns Result.success(emptyList())
        coEvery { repoRepository.getSimilarRepos(any()) } returns Result.success(emptyList())
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
    fun `opening a detail sheet reports the repo's real GitHub star state`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred("batucanx", "reposwipe") } returns true
            val viewModel = viewModel()
            val collectJob = launch { viewModel.detailStarred.collect {} }

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(true, viewModel.detailStarred.value)
            collectJob.cancel()
        }

    @Test
    fun `toggling the detail star on an unstarred repo stars it on GitHub`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            coEvery { starRepository.starRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.detailStarred.collect {} }

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.toggleDetailStar()
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.starRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }

    @Test
    fun `toggling the detail star on an already-starred repo unstars it`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns true
            coEvery { starRepository.unstarRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.detailStarred.collect {} }

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.toggleDetailStar()
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.unstarRepo("batucanx", "reposwipe") }
            collectJob.cancel()
        }

    @Test
    fun `a still-pending local toggle outranks the server's star state`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            every { starRepository.observePendingStarStates() } returns
                flowOf(mapOf("batucanx/reposwipe" to true))
            val viewModel = viewModel()
            val collectJob = launch { viewModel.detailStarred.collect {} }

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(true, viewModel.detailStarred.value)
            collectJob.cancel()
        }

    @Test
    fun `opening a detail sheet loads the language breakdown`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            coEvery { repoRepository.getLanguageBreakdown("batucanx", "reposwipe") } returns
                Result.success(mapOf("Kotlin" to 90_000L, "Java" to 10_000L))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                LanguageBreakdownUiState.Loaded(mapOf("Kotlin" to 90_000L, "Java" to 10_000L)),
                viewModel.languageBreakdownState.value,
            )
        }

    @Test
    fun `a failed language breakdown load surfaces as an error state, not a crash`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            coEvery { repoRepository.getLanguageBreakdown(any(), any()) } returns Result.failure(RuntimeException("offline"))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(LanguageBreakdownUiState.Error, viewModel.languageBreakdownState.value)
        }

    @Test
    fun `opening a detail sheet loads contributors`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            val contributor = Contributor(login = "octocat", avatarUrl = null, contributions = 42, htmlUrl = null)
            coEvery { repoRepository.getContributors("batucanx", "reposwipe") } returns Result.success(listOf(contributor))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(ContributorsUiState.Loaded(listOf(contributor)), viewModel.contributorsState.value)
        }

    @Test
    fun `opening a new detail sheet cancels a still-loading previous repo's contributors load`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            val otherRepo = testRepo.copy(id = 2L, name = "other-repo")
            val contributor = Contributor(login = "octocat", avatarUrl = null, contributions = 42, htmlUrl = null)
            coEvery { repoRepository.getContributors("batucanx", "other-repo") } returns Result.success(listOf(contributor))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            viewModel.onDetailOpened(otherRepo)
            dispatcher.scheduler.advanceUntilIdle()

            // Only the most recently opened repo's contributors should ever land in state.
            assertEquals(ContributorsUiState.Loaded(listOf(contributor)), viewModel.contributorsState.value)
        }

    @Test
    fun `opening a detail sheet loads similar repos`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            val similar = testRepo.copy(id = 2L, name = "similar-repo")
            coEvery { repoRepository.getSimilarRepos(testRepo) } returns Result.success(listOf(similar))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(SimilarReposUiState.Loaded(listOf(similar)), viewModel.similarReposState.value)
        }

    @Test
    fun `a failed similar repos load surfaces as an error state, not a crash`() =
        runTest(dispatcher) {
            coEvery { repoRepository.getReadmeHtml(any(), any()) } returns Result.success(null)
            coEvery { starRepository.isStarred(any(), any()) } returns false
            coEvery { repoRepository.getSimilarRepos(any()) } returns Result.failure(RuntimeException("offline"))
            val viewModel = viewModel()

            viewModel.onDetailOpened(testRepo)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(SimilarReposUiState.Error, viewModel.similarReposState.value)
        }

    @Test
    fun `selectLanguage delegates to the filter repository`() {
        every { discoverFilterRepository.selectLanguage(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.selectLanguage("Rust")

        verify { discoverFilterRepository.selectLanguage("Rust") }
    }
}
