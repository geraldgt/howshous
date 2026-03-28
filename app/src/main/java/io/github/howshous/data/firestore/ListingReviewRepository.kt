package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.ListingReview
import io.github.howshous.data.models.ListingReviewSummary
import kotlinx.coroutines.tasks.await

class ListingReviewRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun listingRef(listingId: String) = db.collection("listings").document(listingId)
    private fun reviewsRef(listingId: String) = listingRef(listingId).collection("reviews")

    suspend fun getReviewsForListing(listingId: String, limit: Long = 20): List<ListingReview> {
        if (listingId.isBlank()) return emptyList()
        return try {
            val snap = reviewsRef(listingId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(ListingReview::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getReviewSummary(listingId: String): ListingReviewSummary? {
        if (listingId.isBlank()) return null
        return try {
            val doc = listingRef(listingId).get().await()
            val listing = doc.toObject(io.github.howshous.data.models.Listing::class.java)
            listing?.reviewSummary
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addReview(listingId: String, review: ListingReview): Boolean {
        if (listingId.isBlank() || review.reviewerId.isBlank()) return false
        return try {
            val reviewDoc = reviewsRef(listingId).document()
            db.runTransaction { txn ->
                val listingSnap = txn.get(listingRef(listingId))
                val summaryMap = listingSnap.get("reviewSummary") as? Map<*, *>
                val existingRecommended = (summaryMap?.get("recommendedCount") as? Number)?.toInt() ?: 0
                val existingNotRecommended = (summaryMap?.get("notRecommendedCount") as? Number)?.toInt() ?: 0
                val existingTotal = (summaryMap?.get("total") as? Number)?.toInt()
                    ?: (existingRecommended + existingNotRecommended)

                val newRecommended = existingRecommended + if (review.recommended) 1 else 0
                val newNotRecommended = existingNotRecommended + if (!review.recommended) 1 else 0
                val newTotal = existingTotal + 1

                val updatedSummary = mapOf(
                    "total" to newTotal,
                    "recommendedCount" to newRecommended,
                    "notRecommendedCount" to newNotRecommended,
                    "updatedAt" to Timestamp.now()
                )

                val reviewData = review.copy(
                    id = "",
                    listingId = listingId,
                    createdAt = review.createdAt ?: Timestamp.now()
                )

                txn.set(reviewDoc, reviewData)
                txn.update(listingRef(listingId), "reviewSummary", updatedSummary)
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
