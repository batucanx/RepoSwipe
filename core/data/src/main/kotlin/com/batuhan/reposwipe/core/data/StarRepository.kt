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
import com.batuhan.reposwipe.core.network.GitHubApiService
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

    /**
     * "owner/repo" -> the star state its *latest* still-pending request would produce, so a screen
     * can reflect a tap before the sync worker confirms it. Latest-wins (rather than "any pending
     * star exists") keeps an entry GitHub repeatedly rejects from outranking newer toggles.
     */
    fun observePendingStarStates(): Flow<Map<String, Boolean>>

    /** Whether GitHub currently reports this repo as starred by the authenticated user. */
    suspend fun isStarred(
        ownerLogin: String,
        repoName: String,
    ): Boolean
}

class StarRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val outboxDao: StarOutboxDao,
        private val api: GitHubApiService,
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
            observePendingStarStates().map { states ->
                states.filterValues { starred -> !starred }.keys
            }

        override fun observePendingStarStates(): Flow<Map<String, Boolean>> =
            outboxDao.observeAll().map { entries ->
                // observeAll() is ordered by createdAt ASC, so folding into a map leaves the most
                // recent action per repo as the winner.
                entries.associate { "${it.ownerLogin}/${it.repoName}" to (it.action == StarAction.STAR.name) }
            }

        override suspend fun isStarred(
            ownerLogin: String,
            repoName: String,
        ): Boolean = api.isRepoStarred(ownerLogin, repoName).isSuccessful

        private suspend fun enqueue(
            ownerLogin: String,
            repoName: String,
            action: StarAction,
        ) {
            // Supersede rather than append: without this, a request GitHub keeps rejecting stays
            // queued forever and the user's later toggles pile up behind it.
            outboxDao.removeAllFor(ownerLogin, repoName)
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
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request =
                OneTimeWorkRequestBuilder<StarSyncWorker>()
                    .setConstraints(constraints)
                    .build()
            // APPEND (not KEEP): a worker already running may have missed an entry added just now,
            // so the next run needs to happen too, rather than being skipped as "already enqueued".
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private companion object {
            const val SYNC_WORK_NAME = "star_sync"
        }
    }
