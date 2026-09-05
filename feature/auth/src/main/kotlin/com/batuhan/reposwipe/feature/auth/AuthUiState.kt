package com.batuhan.reposwipe.feature.auth

import com.batuhan.reposwipe.core.common.text.UiText

sealed interface AuthUiState {
    /** Nothing in flight — shows the "Continue with GitHub" button. Also where sign-in lands back
     * on if the user backs out of the Firebase/GitHub sign-in flow without finishing. */
    data object SignedOut : AuthUiState

    /** Firebase's sign-in flow returned; extracting/saving the GitHub access token. */
    data object SigningIn : AuthUiState

    data object Success : AuthUiState

    data class Error(
        val message: UiText,
    ) : AuthUiState
}
