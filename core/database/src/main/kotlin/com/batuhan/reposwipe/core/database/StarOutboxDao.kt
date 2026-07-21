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
}
