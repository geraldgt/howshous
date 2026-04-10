package io.github.howshous.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.toArgb
import io.github.howshous.data.models.ListingReviewSummary
import io.github.howshous.ui.theme.ReviewBlue
import io.github.howshous.ui.theme.ReviewDarkRed
import io.github.howshous.ui.theme.ReviewGreen
import io.github.howshous.ui.theme.ReviewOrange
import io.github.howshous.ui.theme.ReviewRed
import io.github.howshous.ui.theme.slightlyGray
import io.github.howshous.utils.ReviewSummaryUtils
import java.text.NumberFormat

private val reviewStops = listOf(
    0f to ReviewDarkRed,
    0.25f to ReviewRed,
    0.5f to ReviewOrange,
    0.75f to ReviewGreen,
    1f to ReviewBlue
)

private fun lerpReviewColor(percent: Int): Color {
    val t = (percent.coerceIn(0, 100)) / 100f
    val upperIndex = reviewStops.indexOfFirst { it.first >= t }.let { if (it == -1) reviewStops.lastIndex else it }
    val lowerIndex = (upperIndex - 1).coerceAtLeast(0)
    val (t0, c0) = reviewStops[lowerIndex]
    val (t1, c1) = reviewStops[upperIndex]
    if (t1 == t0) return c1
    val localT = (t - t0) / (t1 - t0)
    return lerpHsv(c0, c1, localT)
}

private fun lerpHsv(from: Color, to: Color, t: Float): Color {
    val tClamped = t.coerceIn(0f, 1f)
    val hsvFrom = FloatArray(3)
    val hsvTo = FloatArray(3)
    AndroidColor.colorToHSV(from.toArgb(), hsvFrom)
    AndroidColor.colorToHSV(to.toArgb(), hsvTo)

    var h0 = hsvFrom[0]
    var h1 = hsvTo[0]
    val dh = h1 - h0
    if (kotlin.math.abs(dh) > 180f) {
        if (h1 > h0) {
            h0 += 360f
        } else {
            h1 += 360f
        }
    }
    val h = (h0 + (h1 - h0) * tClamped) % 360f
    val s = hsvFrom[1] + (hsvTo[1] - hsvFrom[1]) * tClamped
    val v = hsvFrom[2] + (hsvTo[2] - hsvFrom[2]) * tClamped
    return Color(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
}

@Composable
fun ReviewSummaryButton(
    summary: ListingReviewSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = ReviewSummaryUtils.buildDisplay(summary)
    val backgroundColor = if (display.total <= 0) slightlyGray else lerpReviewColor(display.recommendedPercent)

    if (display.total <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No reviews yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = backgroundColor
                )
                Text(
                    "View reviews",
                    style = MaterialTheme.typography.labelSmall,
                    color = backgroundColor.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val numberFormatter = rememberNumberFormatter()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    display.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    " (${display.recommendedPercent}% of ${numberFormatter.format(display.total)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Text(
                "View reviews",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun rememberNumberFormatter(): NumberFormat {
    return remember { NumberFormat.getIntegerInstance() }
}
