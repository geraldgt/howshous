package io.github.howshous.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
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
fun ReviewSummaryRow(
    summary: ListingReviewSummary?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val display = ReviewSummaryUtils.buildDisplay(summary)
    val color = if (display.total <= 0) slightlyGray else lerpReviewColor(display.recommendedPercent)

    if (display.total <= 0) {
        Text(
            "No reviews yet",
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = slightlyGray,
            modifier = modifier
        )
        return
    }

    val numberFormatter = rememberNumberFormatter()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            display.label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = color
        )
        Text(
            " (${display.recommendedPercent}% of ${numberFormatter.format(display.total)})",
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = slightlyGray
        )
    }
}

@Composable
private fun rememberNumberFormatter(): NumberFormat {
    return androidx.compose.runtime.remember { NumberFormat.getIntegerInstance() }
}
