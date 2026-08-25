package com.batuhan.reposwipe.feature.starred

import com.batuhan.reposwipe.core.data.StarRepository
import com.batuhan.reposwipe.core.data.StarredReposRepository
import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StarredViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val starredReposRepository = mockk<StarredReposRepository>()
    private val starRepository = mockk<StarRepository>()
    private val userRepository = mockk<UserRepository>()

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = StarredViewModel(starredReposRepository, starRepository, userRepository)

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
}
