package com.batuhan.reposwipe.core.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.batuhan.reposwipe.core.data.mapper.toDomain
import com.batuhan.reposwipe.core.data.model.User
import com.batuhan.reposwipe.core.database.FollowAction
import com.batuhan.reposwipe.core.database.FollowOutboxDao
import com.batuhan.reposwipe.core.database.FollowOutboxEntity
import com.batuhan.reposwipe.core.network.GitHubApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface FollowRepository {
    suspend fun getFollowers(page: Int): List<User>

    suspend fun getFollowing(page: Int): List<User>

    suspend fun follow(username: String)

    suspend fun unfollow(username: String)

    /**
     * Login -> the follow state its *latest* still-pending request would produce (true = follow,
     * false = unfollow), so a list can reflect a tap before the sync worker confirms it.
     *
     * Deliberately one map rather than two separate sets: with separate "pending follows" /
     * "pending unfollows" sets, a stuck FOLLOW entry (e.g. one GitHub keeps rejecting) would keep
     * winning over every later UNFOLLOW the user taps, and the button could never flip back.
     */
    fun observePendingFollowStates(): Flow<Map<String, Boolean>>

    companion object {
        const val PAGE_SIZE = 30
    }
}

class FollowRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val api: GitHubApiService,
        private val outboxDao: FollowOutboxDao,
    ) : FollowRepository {
        override suspend fun getFollowers(page: Int): List<User> =
            api.getFollowers(page = page, perPage = FollowRepository.PAGE_SIZE).map { it.toDomain() }

        override suspend fun getFollowing(page: Int): List<User> =
            api.getFollowing(page = page, perPage = FollowRepository.PAGE_SIZE).map { it.toDomain() }

        override suspend fun follow(username: String) = enqueue(username, FollowAction.FOLLOW)

        override suspend fun unfollow(username: String) = enqueue(username, FollowAction.UNFOLLOW)

        override fun observePendingFollowStates(): Flow<Map<String, Boolean>> =
            outboxDao.observeAll().map { entries ->
                // observeAll() is ordered by createdAt ASC, so folding into a map leaves the most
                // recent action per username as the winner.
                entries.associate { it.username to (it.action == FollowAction.FOLLOW.name) }
            }

        private suspend fun enqueue(
            username: String,
            action: FollowAction,
        ) {
            // Supersede rather than append: without this, a request GitHub keeps rejecting stays
            // queued forever and the user's later toggles pile up behind it.
            outboxDao.removeAllFor(username)
            outboxDao.enqueue(
                FollowOutboxEntity(
                    username = username,
                    action = action.name,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            scheduleSync()
        }

        private fun scheduleSync() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request =
                OneTimeWorkRequestBuilder<FollowSyncWorker>()
                    .setConstraints(constraints)
                    .build()
            // APPEND (not KEEP): a worker already running may have missed an entry added just now,
            // so the next run needs to happen too, rather than being skipped as "already enqueued".
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private companion object {
            const val SYNC_WORK_NAME = "follow_sync"
        }
    }
