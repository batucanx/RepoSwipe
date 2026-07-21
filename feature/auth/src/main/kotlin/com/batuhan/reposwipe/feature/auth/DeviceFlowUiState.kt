package com.batuhan.reposwipe.feature.auth

sealed interface DeviceFlowUiState {
    data object Loading : DeviceFlowUiState

    data class AwaitingUser(val userCode: String, val verificationUri: String) : DeviceFlowUiState

    data object Success : DeviceFlowUiState

    data class Error(val message: String) : DeviceFlowUiState
}
