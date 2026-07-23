package com.batuhan.reposwipe.feature.profile

import com.batuhan.reposwipe.core.data.UserRepository
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.datastore.TokenDataStore
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val tokenDataStore = mockk<TokenDataStore>()

    private val user = User(login = "batucanx", name = "Batu", avatarUrl = null, publicRepos = 10, followers = 5, following = 3)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load populates the user on success`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } returns user

            val viewModel = ProfileViewModel(userRepository, tokenDataStore)
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

            val viewModel = ProfileViewModel(userRepository, tokenDataStore)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.user == null)
        }

    @Test
    fun `signOut clears the stored access token`() =
        runTest {
            coEvery { userRepository.getCurrentUser() } returns user
            coEvery { tokenDataStore.clearAccessToken() } returns Unit
            val viewModel = ProfileViewModel(userRepository, tokenDataStore)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.signOut()
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { tokenDataStore.clearAccessToken() }
            assertTrue(viewModel.uiState.value.signedOut)
        }
}
