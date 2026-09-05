package com.batuhan.reposwipe.feature.starred

import com.batuhan.reposwipe.core.data.RepoRepository
import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.StarredReposRepository
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StarredViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val starredReposRepository = mockk<StarredReposRepository>()
    private val starRepository = mockk<StarRepository>()
    private val userRepository = mockk<UserRepository>()
    private val repoRepository = mockk<RepoRepository>()

    private val user = User(login = "batucanx", name = "Batu", avatarUrl = null, publicRepos = 10, followers = 5, following = 3)

    private val repo =
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
        every { starRepository.observePendingUnstars() } returns flowOf(emptySet())
        every { starRepository.observePendingStarStates() } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = StarredViewModel(starredReposRepository, starRepository, userRepository, repoRepository)

    @Test
    fun `initial load populates the user and starred repos on success`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns listOf(repo)

            val viewModel = viewModel()
            // uiState is a WhileSubscribed StateFlow — needs a real collector to start combining.
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.error == null)
            assertTrue(state.repos == listOf(repo))
            collectJob.cancel()
        }

    @Test
    fun `a failed repos fetch surfaces an error`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } throws RuntimeException("offline")

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.repos.isEmpty())
            collectJob.cancel()
        }

    @Test
    fun `unstar delegates to StarRepository with the repo's owner and name`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns listOf(repo)
            coEvery { starRepository.unstarRepo(any(), any()) } returns Unit
            val viewModel = viewModel()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.unstar(repo)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { starRepository.unstarRepo("batucanx", "reposwipe") }
        }

    @Test
    fun `refresh is single flight and stops its indicator after success`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns listOf(repo)
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val refreshGate = CompletableDeferred<Unit>()
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } coAnswers {
                refreshGate.await()
                listOf(repo)
            }

            viewModel.refresh()
            viewModel.refresh()
            dispatcher.scheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            refreshGate.complete(Unit)
            dispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            coVerify(exactly = 2) { starredReposRepository.getStarredReposPage(page = 1) }
            coVerify(exactly = 1) { userRepository.getCurrentUser() }
            collectJob.cancel()
        }

    @Test
    fun `failed refresh preserves current repos and exposes a transient error`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns listOf(repo)
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            coEvery { starredReposRepository.getStarredReposPage(page = 1) } throws RuntimeException("offline")
            viewModel.refresh()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertTrue(state.repos == listOf(repo))
            assertNull(state.error)
            assertNotNull(state.transientError)
            collectJob.cancel()
        }

    @Test
    fun `a pending star not yet on the server shows up as a stand-in repo`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { repoRepository.getRepository("batucanx", "reposwipe") } returns Result.success(repo)

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.repos == listOf(repo))
            collectJob.cancel()
        }

    @Test
    fun `a still-loading stand-in fetch does not appear in the list yet`() =
        runTest(dispatcher) {
            val fetchGate = CompletableDeferred<Result<Repo>>()
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { repoRepository.getRepository("batucanx", "reposwipe") } coAnswers { fetchGate.await() }

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.runCurrent()

            val repos = viewModel.uiState.value.repos
            assertTrue(repos.isEmpty())

            fetchGate.complete(Result.success(repo))
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.repos == listOf(repo))
            collectJob.cancel()
        }

    @Test
    fun `a pending star that resolves after the server catches up does not duplicate the repo`() =
        runTest(dispatcher) {
            // The server load and the stand-in fetch race (observePendingStars's combine can fire
            // before load() populates serverRepos), so the stand-in fetch may still happen here —
            // what matters is the settled list shows the repo once, not duplicated.
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns listOf(repo)
            every { starRepository.observePendingStarStates() } returns flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { repoRepository.getRepository("batucanx", "reposwipe") } returns Result.success(repo)

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.repos == listOf(repo))
            collectJob.cancel()
        }

    @Test
    fun `a failed stand-in fetch does not add a repo or crash`() =
        runTest(dispatcher) {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns flowOf(mapOf("batucanx/reposwipe" to true))
            coEvery { repoRepository.getRepository("batucanx", "reposwipe") } returns Result.failure(RuntimeException("offline"))

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val repos = viewModel.uiState.value.repos
            assertTrue(repos.isEmpty())
            collectJob.cancel()
        }

    @Test
    fun `a resolved stand-in is evicted and the screen refreshes once the outbox settles it off-server`() =
        runTest(dispatcher) {
            val pendingStates = MutableStateFlow(mapOf("batucanx/reposwipe" to true))
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { starredReposRepository.getStarredReposPage(page = 1) } returns emptyList()
            every { starRepository.observePendingStarStates() } returns pendingStates
            coEvery { repoRepository.getRepository("batucanx", "reposwipe") } returns Result.success(repo)

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.repos == listOf(repo))

            // The outbox settles (e.g. GitHub ultimately rejected the star) without the repo ever
            // appearing in a real server fetch.
            pendingStates.value = emptyMap()
            dispatcher.scheduler.advanceUntilIdle()

            val repos = viewModel.uiState.value.repos
            assertTrue(repos.isEmpty())
            coVerify(exactly = 2) { starredReposRepository.getStarredReposPage(page = 1) }
            collectJob.cancel()
        }
}
