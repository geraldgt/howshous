package io.github.howshous.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val InputBackground = Color(0xFFF8F8F8)
val InputPlaceholder = Color(0xFF7D7D7D)

// Rounded edges exactly like your Figma fields
val InputShape = RoundedCornerShape(16)

@Composable
fun inputColors() = TextFieldDefaults.colors(
    focusedContainerColor = InputBackground,
    unfocusedContainerColor = InputBackground,
    disabledContainerColor = InputBackground,
    errorContainerColor = InputBackground,

    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,

    cursorColor = Color.Black,
    focusedPlaceholderColor = InputPlaceholder,
    unfocusedPlaceholderColor = InputPlaceholder
)