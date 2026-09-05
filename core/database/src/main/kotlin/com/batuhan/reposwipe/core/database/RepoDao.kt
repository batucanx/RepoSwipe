package com.batuhan.reposwipe.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RepoDao {
    @Query("SELECT * FROM repos WHERE query = :query ORDER BY fetchOrder ASC")
    fun pagingSource(query: String): PagingSource<Int, RepoEntity>

    // The same repo can have one row per distinct `query` it was ever cached under (see
    // RepoEntity's doc) — ORDER BY rowid DESC deterministically prefers the most recently
    // inserted/replaced row instead of an arbitrary one when more than one match exists.
    @Query("SELECT * FROM repos WHERE ownerLogin = :ownerLogin AND name = :name ORDER BY rowid DESC LIMIT 1")
    suspend fun findByOwnerAndName(
        ownerLogin: String,
        name: String,
    ): RepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Query("DELETE FROM repos WHERE query = :query")
    suspend fun clearForQuery(query: String)
}
