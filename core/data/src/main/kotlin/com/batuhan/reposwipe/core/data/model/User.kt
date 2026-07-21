package com.batuhan.reposwipe.core.data.model

data class User(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
)
