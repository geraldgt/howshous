package io.github.howshous.data.models

import com.google.firebase.Timestamp

data class ListingReview(
    val id: String = "",
    val listingId: String = "",
    val reviewerId: String = "",
    val recommended: Boolean = true,
    val comment: String = "",
    val createdAt: Timestamp? = null
)

data class ListingReviewSummary(
    val total: Int = 0,
    val recommendedCount: Int = 0,
    val notRecommendedCount: Int = 0,
    val updatedAt: Timestamp? = null
)
