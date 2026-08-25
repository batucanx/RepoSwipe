package com.batuhan.reposwipe.core.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.batuhan.reposwipe.core.database.FollowAction
import com.batuhan.reposwipe.core.database.FollowOutboxDao
import com.batuhan.reposwipe.core.network.GitHubApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.sentry.Sentry
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Drains the follow/unfollow outbox one entry at a time, spaced out so a burst of taps doesn't
 * hammer GitHub's mutating endpoints. A 403/429 (rate limited) or offline error retries the whole
 * run later via WorkManager's backoff; any other failure (e.g. account deleted) just drops that
 * one entry so it doesn't block the rest of the queue forever.
 */
@HiltWorker
class FollowSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val outboxDao: FollowOutboxDao,
        private val api: GitHubApiService,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val pending = outboxDao.getAll()

            for (entry in pending) {
                try {
                    when (FollowAction.valueOf(entry.action)) {
                        FollowAction.FOLLOW -> api.followUser(entry.username)
                        FollowAction.UNFOLLOW -> api.unfollowUser(entry.username)
                    }
                    outboxDao.remove(entry.id)
                    delay(REQUEST_SPACING_MS)
                } catch (e: HttpException) {
                    if (e.isRateLimited()) {
                        Log.w(TAG, "Rate limited, retrying later", e)
                        return Result.retry()
                    }
                    // Anything else — including a 403 for a token missing the `user:follow` scope —
                    // will fail identically on every retry. Dropping it (and reporting it, since
                    // this is the one place a follow/unfollow can silently never reach GitHub)
                    // keeps a permanently un-syncable entry from pinning the row's optimistic
                    // state forever.
                    Log.w(TAG, "Dropping outbox entry ${entry.id} after non-retryable error", e)
                    Sentry.captureException(e)
                    outboxDao.remove(entry.id)
                } catch (e: IOException) {
                    Log.w(TAG, "Offline, retrying later", e)
                    return Result.retry()
                }
            }

            return Result.success()
        }

        /**
         * GitHub reuses 403 for both "rate limit exhausted" and "your token lacks this scope",
         * so the status code alone can't tell them apart — only a spent `x-ratelimit-remaining`
         * (or an explicit 429) means waiting will actually help.
         */
        private fun HttpException.isRateLimited(): Boolean {
            if (code() == 429) return true
            if (code() != 403) return false
            val remaining = response()?.headers()?.get("x-ratelimit-remaining")?.toIntOrNull()
            return remaining != null && remaining <= 0
        }

        private companion object {
            const val TAG = "FollowSyncWorker"
            const val REQUEST_SPACING_MS = 1_000L
        }
    }
