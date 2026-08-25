package com.batuhan.reposwipe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A pending follow/unfollow request not yet confirmed sent to GitHub. */
@Entity(tableName = "follow_outbox")
data class FollowOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val action: String,
    val createdAt: Long,
)
