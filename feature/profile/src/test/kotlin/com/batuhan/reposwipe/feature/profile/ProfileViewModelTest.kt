package com.batuhan.reposwipe.feature.profile

import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.data.model.User
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val userRepository = mockk<UserRepository>()

    private val user = User(login = "batucanx", name = "Batu", avatarUrl = null, publicRepos = 10, followers = 5, following = 3)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { userRepository.getRecentRepos(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates the user on success`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } returns user

            val viewModel = ProfileViewModel(userRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(user, state.user)
        }

    @Test
    fun `a failed fetch surfaces an error and leaves the user null`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } throws RuntimeException("offline")

            val viewModel = ProfileViewModel(userRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.user == null)
        }

    @Test
    fun `initial load populates recent repos on success`() =
        runTest {
            val repos =
                listOf(
                    Repo(
                        id = 1,
                        name = "Swift-UI-Playground",
                        ownerLogin = "batucanx",
                        ownerAvatarUrl = null,
                        description = "",
                        starCount = 0,
                        forkCount = 0,
                        language = null,
                        updatedAt = "2026-07-25T00:00:00Z",
                        htmlUrl = "https://github.com/batucanx/Swift-UI-Playground",
                        headerImageUrl = "",
                    ),
                )
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { userRepository.getRecentRepos(any()) } returns repos

            val viewModel = ProfileViewModel(userRepository)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(repos, viewModel.uiState.value.recentRepos)
        }

    @Test
    fun `a failed recent repos fetch leaves the list empty without erroring the profile`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { userRepository.getRecentRepos(any()) } throws RuntimeException("offline")

            val viewModel = ProfileViewModel(userRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.recentRepos.isEmpty())
            assertNull(state.error)
        }
}
