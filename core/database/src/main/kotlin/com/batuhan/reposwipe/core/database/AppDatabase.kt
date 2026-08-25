package com.batuhan.reposwipe.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RepoEntity::class, RemoteKeyEntity::class, StarOutboxEntity::class, FollowOutboxEntity::class],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao

    abstract fun remoteKeyDao(): RemoteKeyDao

    abstract fun starOutboxDao(): StarOutboxDao

    abstract fun followOutboxDao(): FollowOutboxDao
}
