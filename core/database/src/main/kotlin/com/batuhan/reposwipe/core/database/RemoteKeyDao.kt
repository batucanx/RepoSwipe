package com.batuhan.reposwipe.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM repo_remote_keys WHERE repoId = :repoId AND query = :query")
    suspend fun remoteKeyByRepoId(
        repoId: Long,
        query: String,
    ): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<RemoteKeyEntity>)

    @Query("DELETE FROM repo_remote_keys WHERE query = :query")
    suspend fun clearForQuery(query: String)
}
