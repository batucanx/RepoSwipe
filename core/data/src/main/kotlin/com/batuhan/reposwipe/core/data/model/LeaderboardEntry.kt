package com.batuhan.reposwipe.core.data.model

data class LeaderboardEntry(
    val repoId: Long,
    val repoName: String,
    val ownerLogin: String,
    val description: String,
    val language: String?,
    val htmlUrl: String,
    val swipeCount: Long,
)
