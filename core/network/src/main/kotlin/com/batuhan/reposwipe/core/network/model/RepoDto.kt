package com.batuhan.reposwipe.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRepositoriesResponseDto(
    val totalCount: Int,
    val incompleteResults: Boolean,
    val items: List<RepoDto>,
)

@Serializable
data class RepoDto(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: OwnerDto,
    val description: String? = null,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String? = null,
    val updatedAt: String,
    val htmlUrl: String,
)

@Serializable
data class OwnerDto(
    val login: String,
    val avatarUrl: String? = null,
)
