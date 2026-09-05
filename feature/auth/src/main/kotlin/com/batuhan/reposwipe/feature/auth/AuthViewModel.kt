package com.batuhan.reposwipe.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.feature.auth.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        /** [AuthScreen] drives the actual sign-in call through this directly — it needs an
         * `Activity`, which this ViewModel must not hold onto. */
        val firebaseAuth: FirebaseAuth get() = authRepository.firebaseAuth

        fun githubProvider(): OAuthProvider = authRepository.githubProvider()

        fun onSignInStarted() {
            _uiState.value = AuthUiState.SigningIn
        }

        /** Resets to [AuthUiState.SignedOut] — the error screen's "Retry" action, and also what
         * [AuthScreen] falls back to if a sign-in attempt never produces a result (the user closed
         * the tab without an explicit cancel signal reaching Firebase). */
        fun retry() {
            _uiState.value = AuthUiState.SignedOut
        }

        fun onSignInResult(result: Result<AuthResult>) {
            result.fold(onSuccess = ::handleAuthResult, onFailure = ::handleAuthFailure)
        }

        private fun handleAuthResult(authResult: AuthResult) {
            val accessToken = (authResult.credential as? OAuthCredential)?.accessToken
            if (accessToken.isNullOrBlank()) {
                _uiState.value = AuthUiState.Error(UiText.Resource(R.string.auth_error_unknown))
                return
            }
            viewModelScope.launch {
                authRepository.saveToken(accessToken)
                _uiState.value = AuthUiState.Success
            }
        }

        @Suppress("TooGenericExceptionCaught") // translated into a user-facing error state below
        private fun handleAuthFailure(error: Throwable) {
            Log.w(TAG, "GitHub sign-in failed", error)
            // The user closing the sign-in tab without finishing surfaces as this specific
            // Firebase exception — treat it as "nothing happened" (back to the button) rather
            // than a scary error message, the same as if they'd never tapped it.
            if (error is FirebaseAuthWebException && error.errorCode == WEB_CONTEXT_CANCELED) {
                _uiState.value = AuthUiState.SignedOut
                return
            }
            if (error is FirebaseNetworkException) {
                _uiState.value = AuthUiState.Error(UiText.Resource(R.string.auth_error_no_internet))
                return
            }
            Sentry.captureException(error)
            val message = error.message?.let { UiText.Dynamic(it) } ?: UiText.Resource(R.string.auth_error_unknown)
            _uiState.value = AuthUiState.Error(message)
        }

        private companion object {
            const val TAG = "AuthViewModel"
            const val WEB_CONTEXT_CANCELED = "ERROR_WEB_CONTEXT_CANCELED"
        }
    }
