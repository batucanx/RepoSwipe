package com.batuhan.reposwipe.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowOutboxDao {
    @Insert
    suspend fun enqueue(entry: FollowOutboxEntity)

    @Query("SELECT * FROM follow_outbox ORDER BY createdAt ASC")
    suspend fun getAll(): List<FollowOutboxEntity>

    @Query("SELECT * FROM follow_outbox ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<FollowOutboxEntity>>

    @Query("DELETE FROM follow_outbox WHERE id = :id")
    suspend fun remove(id: Long)

    /** Drops any still-pending request for this user — a newer toggle supersedes it outright,
     * so the queue can never accumulate contradictory FOLLOW/UNFOLLOW pairs for one account. */
    @Query("DELETE FROM follow_outbox WHERE username = :username")
    suspend fun removeAllFor(username: String)
}
