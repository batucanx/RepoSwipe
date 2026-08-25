package com.batuhan.reposwipe.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val login: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
)
