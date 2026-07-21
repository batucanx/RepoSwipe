package com.batuhan.reposwipe.core.data.mapper

import com.batuhan.reposwipe.core.data.model.Repo
import com.batuhan.reposwipe.core.database.RepoEntity
import com.batuhan.reposwipe.core.network.model.RepoDto

fun RepoDto.toEntity(): RepoEntity =
    RepoEntity(
        id = id,
        name = name,
        fullName = fullName,
        ownerLogin = owner.login,
        ownerAvatarUrl = owner.avatarUrl,
        description = description,
        starCount = stargazersCount,
        forkCount = forksCount,
        language = language,
        updatedAt = updatedAt,
        htmlUrl = htmlUrl,
    )

fun RepoEntity.toDomain(): Repo =
    Repo(
        id = id,
        name = name,
        ownerLogin = ownerLogin,
        ownerAvatarUrl = ownerAvatarUrl,
        description = description.orEmpty(),
        starCount = starCount,
        forkCount = forkCount,
        language = language,
        updatedAt = updatedAt,
        htmlUrl = htmlUrl,
        headerImageUrl = "https://opengraph.githubassets.com/1/$fullName",
    )

/** Direct DTO -> domain mapping for lists that don't go through the Room/Paging cache. */
fun RepoDto.toDomain(): Repo =
    Repo(
        id = id,
        name = name,
        ownerLogin = owner.login,
        ownerAvatarUrl = owner.avatarUrl,
        description = description.orEmpty(),
        starCount = stargazersCount,
        forkCount = forksCount,
        language = language,
        updatedAt = updatedAt,
        htmlUrl = htmlUrl,
        headerImageUrl = "https://opengraph.githubassets.com/1/$fullName",
    )
