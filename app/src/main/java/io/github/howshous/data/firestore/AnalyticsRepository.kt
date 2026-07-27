package io.github.howshous.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.AnalyticsEventType
import io.github.howshous.data.models.SearchFilterKey
import kotlinx.coroutines.tasks.await

/**
 * Centralized analytics event logger used by the client.
 *
 * Responsibilities:
 * - Validate and normalize event types using AnalyticsEventType
 * - Attach session_id (when provided)
 * - Attach a server timestamp
 * - Write to the Firestore `analytics_events` collection
 */
class AnalyticsRepository {
    private val db = FirebaseFirestore.getInstance()

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
                // filter keys (no free-text query)
                "filterKeys" to activeFilterKeys,
                // numeric context for analysis
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
    }
}
