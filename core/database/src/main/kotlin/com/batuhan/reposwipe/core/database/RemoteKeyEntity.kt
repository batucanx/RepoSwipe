package com.batuhan.reposwipe.core.database

import androidx.room.Entity

/**
 * Bookkeeping for [androidx.paging.RemoteMediator] — which page to fetch next per repo row.
 * [query] is part of the key for the same reason it's part of [RepoEntity]'s: a repo can appear
 * under different queries, each with its own independent "next page" cursor.
 */
@Entity(tableName = "repo_remote_keys", primaryKeys = ["repoId", "query"])
data class RemoteKeyEntity(
    val repoId: Long,
    val query: String,
    val nextPage: Int?,
)
