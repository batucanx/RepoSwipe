package com.batuhan.reposwipe.core.data

import com.batuhan.reposwipe.core.data.model.LeaderboardEntry
import com.batuhan.reposwipe.core.data.model.Repo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

interface LeaderboardRepository {
    /** Records a star-swipe against today's global leaderboard (best-effort, offline-queued by Firestore). */
    suspend fun recordSwipe(repo: Repo)

    /** One page of today's leaderboard, ranked by swipe count descending. Pass `reset = true` to start over. */
    suspend fun getLeaderboardPage(
        reset: Boolean,
        pageSize: Int = PAGE_SIZE,
    ): List<LeaderboardEntry>

    companion object {
        const val PAGE_SIZE = 20
    }
}

class LeaderboardRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : LeaderboardRepository {
        private var lastDocument: DocumentSnapshot? = null

        override suspend fun recordSwipe(repo: Repo) {
            val update =
                mapOf(
                    "repoId" to repo.id,
                    "repoName" to repo.name,
                    "ownerLogin" to repo.ownerLogin,
                    "description" to repo.description,
                    "language" to repo.language,
                    "htmlUrl" to repo.htmlUrl,
                    "swipeCount" to FieldValue.increment(1),
                    "lastSwipedAt" to FieldValue.serverTimestamp(),
                )
            entriesCollection().document(repo.id.toString()).set(update, SetOptions.merge()).await()
        }

        override suspend fun getLeaderboardPage(
            reset: Boolean,
            pageSize: Int,
        ): List<LeaderboardEntry> {
            if (reset) lastDocument = null

            var query =
                entriesCollection()
                    .orderBy("swipeCount", Query.Direction.DESCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())
            lastDocument?.let { query = query.startAfter(it) }

            val snapshot = query.get().await()
            lastDocument = snapshot.documents.lastOrNull() ?: lastDocument
            return snapshot.documents.mapNotNull { it.toLeaderboardEntry() }
        }

        private fun entriesCollection() =
            firestore
                .collection(LEADERBOARD_COLLECTION)
                .document(todayDateKey())
                .collection(ENTRIES_COLLECTION)

        private fun todayDateKey(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        private fun DocumentSnapshot.toLeaderboardEntry(): LeaderboardEntry? {
            val repoId = getLong("repoId") ?: return null
            val repoName = getString("repoName") ?: return null
            val ownerLogin = getString("ownerLogin") ?: return null
            val htmlUrl = getString("htmlUrl") ?: return null
            return LeaderboardEntry(
                repoId = repoId,
                repoName = repoName,
                ownerLogin = ownerLogin,
                description = getString("description").orEmpty(),
                language = getString("language"),
                htmlUrl = htmlUrl,
                swipeCount = getLong("swipeCount") ?: 0L,
            )
        }

        private companion object {
            const val LEADERBOARD_COLLECTION = "leaderboard"
            const val ENTRIES_COLLECTION = "entries"
        }
    }
