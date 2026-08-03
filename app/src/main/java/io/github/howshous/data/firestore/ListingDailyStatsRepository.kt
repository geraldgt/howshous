package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

/**
 * Client-side aggregation for listing_daily_stats (Spark plan — no Cloud Functions).
 * Mirrors the dedupe logic in functions/src/index.ts.
 */
class ListingDailyStatsRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun todayUtcDateKey(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private fun dailyRef(listingId: String, date: String) =
        db.collection("listing_daily_stats").document(listingId).collection("days").document(date)

    suspend fun recordView(
        listingId: String,
        landlordId: String,
        sessionId: String,
    ) {
        if (listingId.isBlank() || sessionId.isBlank()) return
        val eventDate = todayUtcDateKey()
        val timestamp = Timestamp.now()
        val metricsRef = db.collection("listing_metrics").document(listingId)
        val sessionRef = metricsRef.collection("sessions").document(sessionId)
        val dailySessionRef = dailyRef(listingId, eventDate).collection("sessions").document(sessionId)
        val dayRef = dailyRef(listingId, eventDate)

        runCatching {
            db.runTransaction { txn ->
                val sessionSnap = txn.get(sessionRef)
                val dailySessionSnap = txn.get(dailySessionRef)

                if (!sessionSnap.exists()) {
                    txn.set(
                        sessionRef,
                        mapOf(
                            "firstViewAt" to timestamp,
                            "eventDate" to eventDate,
                        ),
                        SetOptions.merge()
                    )
                    txn.set(
                        metricsRef,
                        mapOf(
                            "listingId" to listingId,
                            "lastViewedAt" to timestamp,
                            "lastViewedDate" to eventDate,
                            "uniqueSessionViews" to FieldValue.increment(1),
                        ),
                        SetOptions.merge()
                    )
                }

                if (!dailySessionSnap.exists()) {
                    txn.set(
                        dailySessionRef,
                        mapOf(
                            "sessionId" to sessionId,
                            "firstViewAt" to timestamp,
                            "eventDate" to eventDate,
                        )
                    )
                    txn.set(
                        dayRef,
                        mapOf(
                            "listingId" to listingId,
                            "landlordId" to landlordId,
                            "date" to eventDate,
                            "lastViewedAt" to timestamp,
                            "views" to FieldValue.increment(1),
                            "uniqueSessions" to FieldValue.increment(1),
                        ),
                        SetOptions.merge()
                    )
                } else {
                    txn.set(
                        dayRef,
                        mapOf(
                            "listingId" to listingId,
                            "landlordId" to landlordId,
                            "date" to eventDate,
                            "lastViewedAt" to timestamp,
                        ),
                        SetOptions.merge()
                    )
                }
            }.await()
        }.onFailure { it.printStackTrace() }
    }

    suspend fun recordSave(
        listingId: String,
        landlordId: String,
        userId: String,
    ) {
        if (listingId.isBlank() || userId.isBlank()) return
        val eventDate = todayUtcDateKey()
        val timestamp = Timestamp.now()
        val metricsRef = db.collection("listing_metrics").document(listingId)
        val userSaveRef = metricsRef.collection("saves").document(userId)
        val dayRef = dailyRef(listingId, eventDate)

        runCatching {
            db.runTransaction { txn ->
                val saveSnap = txn.get(userSaveRef)
                if (saveSnap.exists()) return@runTransaction

                txn.set(
                    userSaveRef,
                    mapOf(
                        "userId" to userId,
                        "firstSavedAt" to timestamp,
                        "eventDate" to eventDate,
                    )
                )
                txn.set(
                    metricsRef,
                    mapOf(
                        "listingId" to listingId,
                        "lastSavedAt" to timestamp,
                        "lastSavedDate" to eventDate,
                        "totalSaves" to FieldValue.increment(1),
                    ),
                    SetOptions.merge()
                )
                txn.set(
                    dayRef,
                    mapOf(
                        "listingId" to listingId,
                        "landlordId" to landlordId,
                        "date" to eventDate,
                        "lastSavedAt" to timestamp,
                        "saves" to FieldValue.increment(1),
                    ),
                    SetOptions.merge()
                )
            }.await()
        }.onFailure { it.printStackTrace() }
    }

    suspend fun recordMessage(
        listingId: String,
        landlordId: String,
        chatId: String,
    ) {
        if (listingId.isBlank() || chatId.isBlank()) return
        val eventDate = todayUtcDateKey()
        val timestamp = Timestamp.now()
        val metricsRef = db.collection("listing_metrics").document(listingId)
        val chatRef = metricsRef.collection("chats").document(chatId)
        val dayRef = dailyRef(listingId, eventDate)

        runCatching {
            db.runTransaction { txn ->
                val chatSnap = txn.get(chatRef)
                if (chatSnap.exists()) return@runTransaction

                txn.set(
                    chatRef,
                    mapOf(
                        "chatId" to chatId,
                        "firstMessageAt" to timestamp,
                        "eventDate" to eventDate,
                    )
                )
                txn.set(
                    metricsRef,
                    mapOf(
                        "listingId" to listingId,
                        "lastMessageAt" to timestamp,
                        "lastMessageDate" to eventDate,
                        "firstMessageCount" to FieldValue.increment(1),
                    ),
                    SetOptions.merge()
                )
                txn.set(
                    dayRef,
                    mapOf(
                        "listingId" to listingId,
                        "landlordId" to landlordId,
                        "date" to eventDate,
                        "lastMessageAt" to timestamp,
                        "messages" to FieldValue.increment(1),
                    ),
                    SetOptions.merge()
                )
            }.await()
        }.onFailure { it.printStackTrace() }
    }

    /** Backfill save counts from existing saved_listings docs (pre-analytics data). */
    suspend fun backfillSavesFromExisting(listingId: String, landlordId: String) {
        if (listingId.isBlank()) return
        runCatching {
            val saves = db.collectionGroup("saved_listings")
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            for (doc in saves.documents) {
                val userId = doc.reference.parent?.parent?.id ?: continue
                if (userId.isBlank()) continue
                recordSave(listingId, landlordId, userId)
            }
        }.onFailure { it.printStackTrace() }
    }

    /** Backfill message counts from existing chats that already have messages. */
    suspend fun backfillMessagesFromExistingChats(listingId: String, landlordId: String) {
        if (listingId.isBlank() || landlordId.isBlank()) return
        runCatching {
            val chats = db.collection("chats")
                .whereEqualTo("listingId", listingId)
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            for (chatDoc in chats.documents) {
                val chatId = chatDoc.id
                val lastMessage = chatDoc.getString("lastMessage").orEmpty()
                val hasMessages = lastMessage.isNotBlank() ||
                    !db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .limit(1)
                        .get()
                        .await()
                        .isEmpty
                if (hasMessages) {
                    recordMessage(listingId, landlordId, chatId)
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    /** One-time backfill when listings have views but no daily stats yet. */
    suspend fun backfillViewsIfMissing(
        listingId: String,
        landlordId: String,
        uniqueViewCount: Int,
    ) {
        if (listingId.isBlank() || uniqueViewCount <= 0) return
        val days = db.collection("listing_daily_stats")
            .document(listingId)
            .collection("days")
            .limit(1)
            .get()
            .await()
        if (!days.isEmpty) return

        val eventDate = todayUtcDateKey()
        runCatching {
            dailyRef(listingId, eventDate).set(
                mapOf(
                    "listingId" to listingId,
                    "landlordId" to landlordId,
                    "date" to eventDate,
                    "views" to uniqueViewCount,
                    "uniqueSessions" to uniqueViewCount,
                    "saves" to 0,
                    "messages" to 0,
                    "backfilled" to true,
                ),
                SetOptions.merge()
            ).await()
        }.onFailure { it.printStackTrace() }
    }
}
