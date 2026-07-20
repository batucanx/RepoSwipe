package com.batuhan.reposwipe.core.data.model

data class Repo(
    val id: Long,
    val name: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String?,
    val description: String,
    val starCount: Int,
    val forkCount: Int,
    val language: String?,
    val updatedAt: String,
    val htmlUrl: String,
    val headerImageUrl: String,
)
