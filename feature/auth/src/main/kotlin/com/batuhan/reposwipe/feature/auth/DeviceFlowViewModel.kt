package com.batuhan.reposwipe.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.batuhan.reposwipe.feature.auth.data.DeviceCodeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
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

        @Suppress("SwallowedException", "TooGenericExceptionCaught") // translated into a user-facing error state below
        private fun startDeviceFlow() {
            _uiState.value = DeviceFlowUiState.Loading
            viewModelScope.launch {
                if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) {
                    _uiState.value =
                        DeviceFlowUiState.Error(
                            "GitHub Client ID tanımlı değil. local.properties dosyasına " +
                                "github.clientId=... ekleyip projeyi yeniden derle.",
                        )
                    return@launch
                }
                val deviceCode =
                    try {
                        authRepository.requestDeviceCode()
                    } catch (e: IOException) {
                        _uiState.value = DeviceFlowUiState.Error("İnternet bağlantını kontrol et.")
                        return@launch
                    } catch (e: Exception) {
                        _uiState.value = DeviceFlowUiState.Error(e.message ?: "Bilinmeyen bir hata oluştu.")
                        return@launch
                    }
                pollForToken(deviceCode)
            }
        }

        @Suppress("SwallowedException") // transient network blip — kept polling, see comment below
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
                        _uiState.value = DeviceFlowUiState.Error("Kodun süresi doldu, tekrar dene.")
                        return
                    }
                    response.error == "access_denied" -> {
                        _uiState.value = DeviceFlowUiState.Error("Giriş reddedildi.")
                        return
                    }
                    else -> {
                        _uiState.value =
                            DeviceFlowUiState.Error(
                                response.errorDescription ?: "Bilinmeyen bir hata oluştu.",
                            )
                        return
                    }
                }
            }

            _uiState.value = DeviceFlowUiState.Error("Kodun süresi doldu, tekrar dene.")
        }

        private companion object {
            const val SLOW_DOWN_STEP_SECONDS = 5
        }
    }
