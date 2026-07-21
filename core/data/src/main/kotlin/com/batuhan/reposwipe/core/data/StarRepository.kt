package com.batuhan.reposwipe.core.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.batuhan.reposwipe.core.database.StarAction
import com.batuhan.reposwipe.core.database.StarOutboxDao
import com.batuhan.reposwipe.core.database.StarOutboxEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface StarRepository {
    suspend fun starRepo(
        ownerLogin: String,
        repoName: String,
    )

    suspend fun unstarRepo(
        ownerLogin: String,
        repoName: String,
    )

    /** "owner/repo" keys with a not-yet-synced unstar, so lists can hide them optimistically. */
    fun observePendingUnstars(): Flow<Set<String>>
}

class StarRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val outboxDao: StarOutboxDao,
    ) : StarRepository {
        override suspend fun starRepo(
            ownerLogin: String,
            repoName: String,
        ) {
            enqueue(ownerLogin, repoName, StarAction.STAR)
        }

        override suspend fun unstarRepo(
            ownerLogin: String,
            repoName: String,
        ) {
            enqueue(ownerLogin, repoName, StarAction.UNSTAR)
        }

        override fun observePendingUnstars(): Flow<Set<String>> =
            outboxDao.observeAll().map { entries ->
                entries
                    .filter { it.action == StarAction.UNSTAR.name }
                    .map { "${it.ownerLogin}/${it.repoName}" }
                    .toSet()
            }

        private suspend fun enqueue(
            ownerLogin: String,
            repoName: String,
            action: StarAction,
        ) {
            outboxDao.enqueue(
                StarOutboxEntity(
                    ownerLogin = ownerLogin,
                    repoName = repoName,
                    action = action.name,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            scheduleSync()
        }

        private fun scheduleSync() {
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request =
                OneTimeWorkRequestBuilder<StarSyncWorker>()
                    .setConstraints(constraints)
                    .build()
            // APPEND (not KEEP): a worker already running may have missed an entry added just now,
            // so the next run needs to happen too, rather than being skipped as "already enqueued".
            WorkManager.getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private companion object {
            const val SYNC_WORK_NAME = "star_sync"
        }
    }
