package io.github.howshous.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.howshous.R


val Actor = FontFamily(
    Font(R.font.actor)
)

val Roboto = FontFamily(
    Font(R.font.roboto_regular)
)

val RobotoBold = FontFamily(
    Font(R.font.roboto_bold, FontWeight.Bold)
)

val RobotoCondensed = FontFamily(
    Font(R.font.roboto_condensed)
)

val RobotoMono = FontFamily(
    Font(R.font.roboto_mono)
)

val AppTypography = Typography(

    // BODY TEXT
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Roboto,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    // HEADLINES
    headlineLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontSize = 32.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RobotoCondensed,
        fontSize = 28.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RobotoCondensed,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),

    // TITLES
    titleLarge = TextStyle(
        fontFamily = RobotoBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RobotoBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RobotoBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),

    // LABELS
    labelLarge = TextStyle(
        fontFamily = Actor,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Actor,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Actor,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    // NUMERIC
    displaySmall = TextStyle(
        fontFamily = RobotoMono,
        fontSize = 20.sp,
        lineHeight = 22.sp
    )
)