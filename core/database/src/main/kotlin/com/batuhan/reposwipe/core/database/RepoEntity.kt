package com.batuhan.reposwipe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String?,
    val description: String?,
    val starCount: Int,
    val forkCount: Int,
    val language: String?,
    val updatedAt: String,
    val htmlUrl: String,
)
