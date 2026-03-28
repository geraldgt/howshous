package io.github.howshous.utils

import io.github.howshous.data.models.ListingReviewSummary
import kotlin.math.roundToInt

enum class ReviewSentiment {
    NONE,
    POSITIVE,
    MIXED,
    NEGATIVE
}

data class ReviewSummaryDisplay(
    val total: Int,
    val recommendedPercent: Int,
    val label: String,
    val sentiment: ReviewSentiment
)

object ReviewSummaryUtils {
    fun buildDisplay(summary: ListingReviewSummary?): ReviewSummaryDisplay {
        if (summary == null) {
            return ReviewSummaryDisplay(
                total = 0,
                recommendedPercent = 0,
                label = "No reviews yet",
                sentiment = ReviewSentiment.NONE
            )
        }

        val recommended = summary.recommendedCount
        val notRecommended = summary.notRecommendedCount
        val total = if (summary.total > 0) summary.total else recommended + notRecommended

        if (total <= 0) {
            return ReviewSummaryDisplay(
                total = 0,
                recommendedPercent = 0,
                label = "No reviews yet",
                sentiment = ReviewSentiment.NONE
            )
        }

        val percent = ((recommended.toFloat() / total) * 100f).roundToInt()
        val label = when {
            percent >= 95 -> "Overwhelmingly Positive"
            percent >= 90 -> "Very Positive"
            percent >= 80 -> "Positive"
            percent >= 70 -> "Mostly Positive"
            percent >= 40 -> "Mixed"
            percent >= 20 -> "Mostly Negative"
            else -> "Overwhelmingly Negative"
        }
        val sentiment = when {
            percent >= 70 -> ReviewSentiment.POSITIVE
            percent >= 40 -> ReviewSentiment.MIXED
            else -> ReviewSentiment.NEGATIVE
        }

        return ReviewSummaryDisplay(
            total = total,
            recommendedPercent = percent,
            label = label,
            sentiment = sentiment
        )
    }
}
