package com.batuhan.reposwipe.feature.auth.data

import kotlinx.serialization.Serializable

@Serializable
data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
)

@Serializable
data class AccessTokenResponse(
    val accessToken: String? = null,
    val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val errorDescription: String? = null,
)
