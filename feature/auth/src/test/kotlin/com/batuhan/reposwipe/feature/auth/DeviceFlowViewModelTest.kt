package com.batuhan.reposwipe.feature.auth

import android.util.Log
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.feature.auth.data.AccessTokenResponse
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.batuhan.reposwipe.feature.auth.data.DeviceCodeResponse
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
import java.io.IOException

/**
 * Only covers the deterministic branches. The while-loop inside pollForToken keys its expiry off
 * [System.currentTimeMillis], not the virtual test clock, so exhaustively testing "polls N times
 * then expires" isn't reliable here without refactoring the ViewModel to take an injectable clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFlowViewModelTest {
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

    @Test
    fun `an access token on the first poll moves to Success and saves the token`() =
        runTest {
            val deviceCode =
                DeviceCodeResponse(
                    deviceCode = "device-code",
                    userCode = "ABC-123",
                    verificationUri = "https://github.com/login/device",
                    expiresIn = 900,
                    interval = 5,
                )
            coEvery { authRepository.requestDeviceCode() } returns deviceCode
            coEvery { authRepository.pollAccessToken(deviceCode.deviceCode) } returns
                AccessTokenResponse(accessToken = "ghu_token")
            coEvery { authRepository.saveToken(any()) } returns Unit

            val viewModel = DeviceFlowViewModel(authRepository)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(DeviceFlowUiState.Success, viewModel.uiState.value)
            coVerify { authRepository.saveToken("ghu_token") }
        }

    @Test
    fun `a network failure requesting the device code surfaces a no-internet error`() =
        runTest {
            coEvery { authRepository.requestDeviceCode() } throws IOException("no network")

            val viewModel = DeviceFlowViewModel(authRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is DeviceFlowUiState.Error)
            assertEquals(
                UiText.Resource(R.string.auth_error_no_internet),
                (state as DeviceFlowUiState.Error).message,
            )
        }

    @Test
    fun `an expired_token poll response surfaces an expired error`() =
        runTest {
            val deviceCode =
                DeviceCodeResponse(
                    deviceCode = "device-code",
                    userCode = "ABC-123",
                    verificationUri = "https://github.com/login/device",
                    expiresIn = 900,
                    interval = 5,
                )
            coEvery { authRepository.requestDeviceCode() } returns deviceCode
            coEvery { authRepository.pollAccessToken(deviceCode.deviceCode) } returns
                AccessTokenResponse(error = "expired_token")

            val viewModel = DeviceFlowViewModel(authRepository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is DeviceFlowUiState.Error)
            assertEquals(
                UiText.Resource(R.string.auth_error_expired),
                (state as DeviceFlowUiState.Error).message,
            )
        }
}
