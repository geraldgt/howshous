package io.github.howshous.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.AnalyticsEventType
import io.github.howshous.data.models.SearchFilterKey
import kotlinx.coroutines.tasks.await

/**
 * Logs analytics events and aggregates listing_daily_stats on the client
 * (Spark plan — Cloud Functions are not used).
 */
class AnalyticsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val dailyStatsRepo = ListingDailyStatsRepository()

    private fun eventsCollection() = db.collection("events")

    private suspend fun logEvent(
        type: AnalyticsEventType,
        userId: String?,
        sessionId: String?,
        payload: Map<String, Any?>,
    ) {
        val data = mutableMapOf<String, Any?>(
            "eventType" to type.value,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        if (!userId.isNullOrBlank()) {
            data["userId"] = userId
        }
        if (!sessionId.isNullOrBlank()) {
            data["sessionId"] = sessionId
        }

        payload.forEach { (key, value) ->
            if (value != null) {
                data[key] = value
            }
        }

        runCatching { eventsCollection().add(data).await() }
    }

    suspend fun logListingView(
        listingId: String,
        landlordId: String,
        userId: String,
        sessionId: String?,
        price: Int?,
    ) {
        if (listingId.isBlank() || landlordId.isBlank() || userId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_VIEW,
            userId = userId,
            sessionId = sessionId,
            payload = mapOf(
                "listingId" to listingId,
                "landlordId" to landlordId,
                "price" to price,
            ),
        )

        if (!sessionId.isNullOrBlank()) {
            dailyStatsRepo.recordView(listingId, landlordId, sessionId)
        }
    }

    suspend fun logSearchFilters(
        userId: String,
        sessionId: String?,
        hasQuery: Boolean,
        minPrice: Int?,
        maxPrice: Int?,
        amenities: Set<String>,
    ) {
        if (userId.isBlank() && sessionId.isNullOrBlank()) return

        val activeFilterKeys = mutableListOf<String>()
        if (hasQuery) activeFilterKeys += SearchFilterKey.QUERY.value
        if (minPrice != null) activeFilterKeys += SearchFilterKey.MIN_PRICE.value
        if (maxPrice != null) activeFilterKeys += SearchFilterKey.MAX_PRICE.value
        amenities.forEach { amenity ->
            activeFilterKeys += SearchFilterKey.amenityKey(amenity)
        }

        logEvent(
            type = AnalyticsEventType.SEARCH_PERFORMED,
            userId = userId.ifBlank { null },
            sessionId = sessionId,
            payload = mapOf(
                "filterKeys" to activeFilterKeys,
                "minPrice" to (minPrice ?: 0),
                "maxPrice" to (maxPrice ?: 0),
                "amenities" to amenities.toList(),
            ),
        )
    }

    suspend fun logMessageSent(
        chatId: String,
        listingId: String,
        landlordId: String,
        senderId: String,
    ) {
        if (chatId.isBlank() || listingId.isBlank() || landlordId.isBlank() || senderId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_MESSAGE,
            userId = senderId,
            sessionId = null,
            payload = mapOf(
                "chatId" to chatId,
                "listingId" to listingId,
                "landlordId" to landlordId,
            ),
        )

        dailyStatsRepo.recordMessage(listingId, landlordId, chatId)
    }

    suspend fun logListingSave(
        listingId: String,
        landlordId: String,
        userId: String,
        sessionId: String?,
        price: Int?,
    ) {
        if (listingId.isBlank() || landlordId.isBlank() || userId.isBlank()) return

        logEvent(
            type = AnalyticsEventType.LISTING_SAVE,
            userId = userId,
            sessionId = sessionId,
            payload = mapOf(
                "listingId" to listingId,
                "landlordId" to landlordId,
                "price" to price,
            ),
        )

        dailyStatsRepo.recordSave(listingId, landlordId, userId)
    }

    /** Sync analytics for a listing the user already saved before aggregation existed. */
    suspend fun syncExistingSave(
        listingId: String,
        landlordId: String,
        userId: String,
    ) {
        if (listingId.isBlank() || landlordId.isBlank() || userId.isBlank()) return
        dailyStatsRepo.recordSave(listingId, landlordId, userId)
    }

    /** Sync analytics for a chat that already had messages before aggregation existed. */
    suspend fun syncExistingMessage(
        chatId: String,
        listingId: String,
        landlordId: String,
    ) {
        if (chatId.isBlank() || listingId.isBlank() || landlordId.isBlank()) return
        dailyStatsRepo.recordMessage(listingId, landlordId, chatId)
    }
}
