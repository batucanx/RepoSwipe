package com.batuhan.reposwipe.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.batuhan.reposwipe.feature.auth.data.DeviceCodeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DeviceFlowViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DeviceFlowUiState>(DeviceFlowUiState.Loading)
        val uiState: StateFlow<DeviceFlowUiState> = _uiState.asStateFlow()

        init {
            startDeviceFlow()
        }

        fun retry() = startDeviceFlow()

        @Suppress("TooGenericExceptionCaught") // translated into a user-facing error state below
        private fun startDeviceFlow() {
            _uiState.value = DeviceFlowUiState.Loading
            viewModelScope.launch {
                if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) {
                    _uiState.value = DeviceFlowUiState.Error(UiText.Resource(R.string.auth_error_missing_client_id))
                    return@launch
                }
                val deviceCode =
                    try {
                        authRepository.requestDeviceCode()
                    } catch (e: IOException) {
                        Log.w(TAG, "requestDeviceCode failed", e)
                        _uiState.value = DeviceFlowUiState.Error(UiText.Resource(R.string.auth_error_no_internet))
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "requestDeviceCode failed", e)
                        Sentry.captureException(e)
                        val message = e.message?.let { UiText.Dynamic(it) } ?: UiText.Resource(R.string.auth_error_unknown)
                        _uiState.value = DeviceFlowUiState.Error(message)
                        return@launch
                    }
                pollForToken(deviceCode)
            }
        }

        private suspend fun pollForToken(deviceCode: DeviceCodeResponse) {
            _uiState.value =
                DeviceFlowUiState.AwaitingUser(
                    userCode = deviceCode.userCode,
                    verificationUri = deviceCode.verificationUri,
                )

            var intervalSeconds = deviceCode.interval
            val deadlineMillis = System.currentTimeMillis() + deviceCode.expiresIn * 1000L

            while (System.currentTimeMillis() < deadlineMillis) {
                delay(intervalSeconds * 1000L)

                val response =
                    try {
                        authRepository.pollAccessToken(deviceCode.deviceCode)
                    } catch (e: IOException) {
                        // Transient network blip (DNS hiccup, emulator losing connectivity while the
                        // browser had focus, ...). GitHub-side authorization may already be done, so
                        // keep polling instead of failing the whole flow on a single dropped request.
                        Log.w(TAG, "pollAccessToken failed, retrying", e)
                        continue
                    }

                when {
                    !response.accessToken.isNullOrBlank() -> {
                        authRepository.saveToken(response.accessToken)
                        _uiState.value = DeviceFlowUiState.Success
                        return
                    }
                    response.error == "authorization_pending" -> Unit
                    response.error == "slow_down" -> intervalSeconds += SLOW_DOWN_STEP_SECONDS
                    response.error == "expired_token" -> {
                        _uiState.value = DeviceFlowUiState.Error(UiText.Resource(R.string.auth_error_expired))
                        return
                    }
                    response.error == "access_denied" -> {
                        _uiState.value = DeviceFlowUiState.Error(UiText.Resource(R.string.auth_error_access_denied))
                        return
                    }
                    else -> {
                        val message =
                            response.errorDescription?.let { UiText.Dynamic(it) }
                                ?: UiText.Resource(R.string.auth_error_unknown)
                        _uiState.value = DeviceFlowUiState.Error(message)
                        return
                    }
                }
            }

            _uiState.value = DeviceFlowUiState.Error(UiText.Resource(R.string.auth_error_expired))
        }

        private companion object {
            const val TAG = "DeviceFlowViewModel"
            const val SLOW_DOWN_STEP_SECONDS = 5
        }
    }
