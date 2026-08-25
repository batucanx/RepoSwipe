package com.batuhan.reposwipe.feature.followers

import androidx.lifecycle.SavedStateHandle
import com.batuhan.reposwipe.core.data.FollowRepository
import com.batuhan.reposwipe.core.data.UserRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val followRepository = mockk<FollowRepository>()
    private val userRepository = mockk<UserRepository>()
    private val savedStateHandle = mockk<SavedStateHandle>()

    private val follower =
        User(login = "octocat", name = "The Octocat", avatarUrl = null, publicRepos = 8, followers = 100, following = 9)
    private val followed =
        User(login = "batucanx", name = "Batu", avatarUrl = null, publicRepos = 10, followers = 5, following = 3)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { savedStateHandle.get<String>(any()) } returns null
        every { followRepository.observePendingFollowStates() } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PeopleViewModel(savedStateHandle, followRepository, userRepository)

    @Test
    fun `initial load populates followers and following on success`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns listOf(follower)
            coEvery { followRepository.getFollowing(page = 1) } returns listOf(followed)
            coEvery { userRepository.getAllFollowingLogins() } returns setOf(followed.login)

            val viewModel = viewModel()
            // uiState is a WhileSubscribed StateFlow — needs a real collector to start combining.
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.error == null)
            assertEquals(1, state.followers.size)
            assertEquals("octocat", state.followers.first().login)
            assertFalse(state.followers.first().isFollowing)
            assertEquals(1, state.following.size)
            assertTrue(state.following.first().isFollowing)
            collectJob.cancel()
        }

    @Test
    fun `a follower already followed back is cross-referenced from the following set`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns listOf(follower)
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns setOf(follower.login)

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.followers
                    .first()
                    .isFollowing,
            )
            collectJob.cancel()
        }

    @Test
    fun `a failed followers fetch surfaces an error`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } throws RuntimeException("offline")
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            collectJob.cancel()
        }

    @Test
    fun `toggling follow on a not-yet-followed user calls follow`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns listOf(follower)
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()
            coEvery { followRepository.follow(any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleFollow(
                viewModel.uiState.value.followers
                    .first(),
            )
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { followRepository.follow("octocat") }
            collectJob.cancel()
        }

    @Test
    fun `toggling follow on an already-followed user calls unfollow`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns emptyList()
            coEvery { followRepository.getFollowing(page = 1) } returns listOf(followed)
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()
            coEvery { followRepository.unfollow(any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleFollow(
                viewModel.uiState.value.following
                    .first(),
            )
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { followRepository.unfollow("batucanx") }
            collectJob.cancel()
        }

    @Test
    fun `following a user from the Followers tab immediately adds them to the Following tab`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns listOf(follower)
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()
            coEvery { followRepository.follow(any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleFollow(
                viewModel.uiState.value.followers
                    .first(),
            )
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.following.any { it.login == "octocat" })
            assertTrue(state.followers.first().isFollowing)
            collectJob.cancel()
        }

    @Test
    fun `unfollowing a user from the Following tab immediately removes them from it`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns emptyList()
            coEvery { followRepository.getFollowing(page = 1) } returns listOf(followed)
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()
            coEvery { followRepository.unfollow(any()) } returns Unit
            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.toggleFollow(
                viewModel.uiState.value.following
                    .first(),
            )
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.following
                    .none { it.login == "batucanx" },
            )
            collectJob.cancel()
        }

    @Test
    fun `a pending unfollow wins over the server-reported following state`() =
        runTest(dispatcher) {
            // Regression: pending follow/unfollow used to be two separate sets checked in a fixed
            // order, so a stuck FOLLOW entry outranked every later UNFOLLOW and the button froze.
            every { followRepository.observePendingFollowStates() } returns flowOf(mapOf("octocat" to false))
            coEvery { followRepository.getFollowers(page = 1) } returns listOf(follower)
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns setOf(follower.login)

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.followers
                    .first()
                    .isFollowing,
            )
            collectJob.cancel()
        }

    @Test
    fun `selectTab switches which list is exposed as selectedTab`() =
        runTest(dispatcher) {
            coEvery { followRepository.getFollowers(page = 1) } returns emptyList()
            coEvery { followRepository.getFollowing(page = 1) } returns emptyList()
            coEvery { userRepository.getAllFollowingLogins() } returns emptySet()

            val viewModel = viewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(PeopleTab.FOLLOWERS, viewModel.uiState.value.selectedTab)
            viewModel.selectTab(PeopleTab.FOLLOWING)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(PeopleTab.FOLLOWING, viewModel.uiState.value.selectedTab)
            collectJob.cancel()
        }
}
