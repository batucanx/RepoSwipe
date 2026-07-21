package com.batuhan.reposwipe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A pending star/unstar request not yet confirmed sent to GitHub. */
@Entity(tableName = "star_outbox")
data class StarOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerLogin: String,
    val repoName: String,
    val action: String,
    val createdAt: Long,
)
