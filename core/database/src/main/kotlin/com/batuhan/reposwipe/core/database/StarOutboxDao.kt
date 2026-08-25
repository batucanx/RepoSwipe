package com.batuhan.reposwipe.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StarOutboxDao {
    @Insert
    suspend fun enqueue(entry: StarOutboxEntity)

    @Query("SELECT * FROM star_outbox ORDER BY createdAt ASC")
    suspend fun getAll(): List<StarOutboxEntity>

    @Query("SELECT * FROM star_outbox ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<StarOutboxEntity>>

    @Query("DELETE FROM star_outbox WHERE id = :id")
    suspend fun remove(id: Long)

    /** Drops any still-pending request for this repo — a newer toggle supersedes it outright,
     * so the queue can never accumulate contradictory STAR/UNSTAR pairs for one repo. */
    @Query("DELETE FROM star_outbox WHERE ownerLogin = :ownerLogin AND repoName = :repoName")
    suspend fun removeAllFor(
        ownerLogin: String,
        repoName: String,
    )
}
