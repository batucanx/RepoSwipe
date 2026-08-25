package com.batuhan.reposwipe.core.data.mapper

import com.batuhan.reposwipe.core.data.model.Contributor
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.network.model.ContributorDto
import com.batuhan.reposwipe.core.network.model.UserDto

fun UserDto.toDomain(): User =
    User(
        login = login,
        name = name,
        avatarUrl = avatarUrl,
        publicRepos = publicRepos,
        followers = followers,
        following = following,
    )

fun ContributorDto.toDomain(): Contributor =
    Contributor(
        login = login,
        avatarUrl = avatarUrl,
        contributions = contributions,
        htmlUrl = htmlUrl,
    )
