package com.batuhan.reposwipe.core.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.batuhan.reposwipe.core.database.StarAction
import com.batuhan.reposwipe.core.database.StarOutboxDao
import com.batuhan.reposwipe.core.network.GitHubApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Drains the star/unstar outbox one entry at a time, spaced out so a burst of swipes doesn't
 * hammer GitHub's mutating endpoints. A 403/429 (rate limited) or offline error retries the
 * whole run later via WorkManager's backoff; any other failure (e.g. repo deleted) just drops
 * that one entry so it doesn't block the rest of the queue forever.
 */
@HiltWorker
class StarSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val outboxDao: StarOutboxDao,
        private val api: GitHubApiService,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val pending = outboxDao.getAll()

            for (entry in pending) {
                try {
                    when (StarAction.valueOf(entry.action)) {
                        StarAction.STAR -> api.starRepo(entry.ownerLogin, entry.repoName)
                        StarAction.UNSTAR -> api.unstarRepo(entry.ownerLogin, entry.repoName)
                    }
                    outboxDao.remove(entry.id)
                    delay(REQUEST_SPACING_MS)
                } catch (e: HttpException) {
                    if (e.code() == 403 || e.code() == 429) {
                        Log.w(TAG, "Rate limited, retrying later", e)
                        return Result.retry()
                    }
                    Log.w(TAG, "Dropping outbox entry ${entry.id} after non-retryable error", e)
                    outboxDao.remove(entry.id)
                } catch (e: IOException) {
                    Log.w(TAG, "Offline, retrying later", e)
                    return Result.retry()
                }
            }

            return Result.success()
        }

        private companion object {
            const val TAG = "StarSyncWorker"
            const val REQUEST_SPACING_MS = 1_000L
        }
    }
