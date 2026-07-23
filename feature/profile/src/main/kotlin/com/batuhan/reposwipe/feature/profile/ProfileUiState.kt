package com.batuhan.reposwipe.feature.profile

import com.batuhan.reposwipe.core.common.text.UiText
import com.batuhan.reposwipe.core.data.model.User

data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val user: User? = null,
    val signedOut: Boolean = false,
)
