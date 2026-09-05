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
    val topics: List<String> = emptyList(),
)

/** The "owner/repo" string used as a map/set key for this repo's star state across the outbox. */
val Repo.ownerRepoKey: String
    get() = "$ownerLogin/$name"

/** Splits an [ownerRepoKey]-shaped string back into (ownerLogin, repoName), or null if malformed. */
fun ownerRepoKeyParts(key: String): Pair<String, String>? {
    val separatorIndex = key.indexOf('/')
    return if (separatorIndex <= 0) null else key.substring(0, separatorIndex) to key.substring(separatorIndex + 1)
}
