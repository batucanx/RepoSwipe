package com.batuhan.reposwipe.feature.auth

import android.util.Log
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun authResultWithToken(token: String?): AuthResult {
        val oauthCredential = mockk<OAuthCredential> { every { accessToken } returns token }
        return mockk<AuthResult> { every { credential } returns oauthCredential }
    }

    @Test
    fun `a successful sign-in saves the GitHub access token and moves to Success`() =
        runTest(dispatcher) {
            coEvery { authRepository.saveToken("ghu_token") } returns Unit
            val viewModel = AuthViewModel(authRepository)

            viewModel.onSignInResult(Result.success(authResultWithToken("ghu_token")))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(AuthUiState.Success, viewModel.uiState.value)
            coVerify { authRepository.saveToken("ghu_token") }
        }

    @Test
    fun `a result with no access token surfaces an unknown error`() =
        runTest(dispatcher) {
            val viewModel = AuthViewModel(authRepository)

            viewModel.onSignInResult(Result.success(authResultWithToken(null)))
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AuthUiState.Error)
            assertEquals(UiText.Resource(R.string.auth_error_unknown), (state as AuthUiState.Error).message)
            coVerify(exactly = 0) { authRepository.saveToken(any()) }
        }

    @Test
    fun `the user canceling the sign-in tab resets to SignedOut, not an error`() {
        val cancellation = mockk<FirebaseAuthWebException> { every { errorCode } returns "ERROR_WEB_CONTEXT_CANCELED" }
        val viewModel = AuthViewModel(authRepository)

        viewModel.onSignInResult(Result.failure(cancellation))

        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun `a network failure surfaces a no-internet error`() {
        val viewModel = AuthViewModel(authRepository)

        viewModel.onSignInResult(Result.failure(mockk<FirebaseNetworkException>()))

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(UiText.Resource(R.string.auth_error_no_internet), (state as AuthUiState.Error).message)
    }

    @Test
    fun `any other failure surfaces its message as a dynamic error`() {
        val viewModel = AuthViewModel(authRepository)

        viewModel.onSignInResult(Result.failure(RuntimeException("something went wrong")))

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(UiText.Dynamic("something went wrong"), (state as AuthUiState.Error).message)
    }

    @Test
    fun `retry resets an error state back to SignedOut`() {
        val viewModel = AuthViewModel(authRepository)
        viewModel.onSignInResult(Result.failure(RuntimeException("boom")))
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        viewModel.retry()

        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
    }
}
