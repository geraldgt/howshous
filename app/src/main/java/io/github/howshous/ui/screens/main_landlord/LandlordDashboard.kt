package io.github.howshous.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.ui.data.clearSession
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.data.readUidFlow
import kotlinx.coroutines.launch

@Composable
fun LandlordDashboard(nav: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uid by readUidFlow(context).collectAsState(initial = "")
    val role by readRoleFlow(context).collectAsState(initial = "")

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Landlord Dashboard", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        Text("UID: $uid")
        Text("Role: $role")

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    clearSession(context)
                    nav.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        ) {
            Text("Logout")
        }
    }
}
