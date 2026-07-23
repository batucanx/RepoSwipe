package com.batuhan.reposwipe.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RepoEntity::class, RemoteKeyEntity::class, StarOutboxEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao

    abstract fun remoteKeyDao(): RemoteKeyDao

    abstract fun starOutboxDao(): StarOutboxDao
}
