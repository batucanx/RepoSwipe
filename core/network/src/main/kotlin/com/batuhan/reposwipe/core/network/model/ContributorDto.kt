package com.batuhan.reposwipe.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ContributorDto(
    val login: String,
    val avatarUrl: String? = null,
    val contributions: Int = 0,
    val htmlUrl: String? = null,
)
