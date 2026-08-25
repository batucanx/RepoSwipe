package com.batuhan.reposwipe.core.database

import androidx.room.Entity
import androidx.room.Index

/**
 * [query] is the exact GitHub search query string this row was fetched for, and is part of the
 * primary key — without it, switching Discover filters (e.g. selecting a different language)
 * could see a slow, superseded query's network response land *after* the new query's own
 * refresh and silently overwrite the fresh rows in the shared table, since a single unscoped
 * cache has no way to tell "belongs to the old query" apart from "belongs to the new one".
 *
 * [fetchOrder] captures the (randomized) order rows were fetched/shuffled in by
 * [com.batuhan.reposwipe.core.data.RepoRemoteMediator] — [RepoDao.pagingSource] sorts by this
 * instead of [starCount] so the swipe deck shows real variety instead of always the same
 * highest-starred repos first.
 */
@Entity(tableName = "repos", primaryKeys = ["id", "query"], indices = [Index("fetchOrder"), Index("query")])
data class RepoEntity(
    val id: Long,
    val query: String,
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
    val fetchOrder: Int,
    val topics: List<String>,
)
