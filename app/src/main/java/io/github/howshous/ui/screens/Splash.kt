package io.github.howshous.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay

@Composable
fun Splash(nav: NavController) {

    // Auto-navigate to login after a short delay
    LaunchedEffect(Unit) {
        delay(1200)                 // splash duration
        nav.navigate("login_choice") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // UI layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryTeal),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_white),
            contentDescription = "HowsHous Logo",
            modifier = Modifier
                .size(200.dp)
        )
    }
}