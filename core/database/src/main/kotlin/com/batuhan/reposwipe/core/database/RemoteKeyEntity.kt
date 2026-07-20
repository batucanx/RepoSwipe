package com.batuhan.reposwipe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bookkeeping for [androidx.paging.RemoteMediator] — which page to fetch next per repo row. */
@Entity(tableName = "repo_remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val repoId: Long,
    val nextPage: Int?,
)
