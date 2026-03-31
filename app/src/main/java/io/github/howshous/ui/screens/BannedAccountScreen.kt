import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.data.auth.AuthRepository
import io.github.howshous.data.firestore.BanAppealRepository
import io.github.howshous.data.models.BanAppeal
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.saveRole
import io.github.howshous.ui.theme.SurfaceLight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
@Composable
fun BannedAccountScreen(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val appealRepo = remember { BanAppealRepository() }
    val scope = rememberCoroutineScope()

    var appeals by remember { mutableStateOf<List<BanAppeal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var appealMessage by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var lastRefreshTime by remember { mutableStateOf(0L) }
    var hasNavigated by remember { mutableStateOf(false) }
    var banReason by remember { mutableStateOf("Not specified") }
    var bannedAt by remember { mutableStateOf("Unknown") }

    fun refresh() {
        if (uid.isBlank() || hasNavigated) return
        val now = System.currentTimeMillis()
        // Prevent refresh from being called more than once every 5 seconds
        if (now - lastRefreshTime < 5000) return
        lastRefreshTime = now

        scope.launch {
            try {
                isLoading = true
                
                // Fetch ban info directly from Firestore to avoid deserialization errors
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(uid).get().await()
                val isBanned = userDoc.getBoolean("isBanned") ?: true
                val role = userDoc.getString("role") ?: "banned"
                
                // Update ban info
                banReason = userDoc.getString("banReason") ?: "Not specified"
                bannedAt = userDoc.getTimestamp("bannedAt")?.toDate()?.toString() ?: "Unknown"
                
                // If ban has been lifted, navigate away
                if (!isBanned && role != "banned" && !hasNavigated) {
                    saveRole(context, role)
                    hasNavigated = true
                    nav.navigate("dashboard_router") {
                        launchSingleTop = true
                    }
                    return@launch
                }
                
                // Fetch appeals
                appeals = appealRepo.getAppealsForUser(uid)
                isLoading = false
            } catch (e: Exception) {
                // Silently handle errors to prevent loops
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    LaunchedEffect(uid) {
        hasNavigated = false
        lastRefreshTime = 0
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Account Restricted", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your account has been banned. You can submit an appeal below.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Reason: ${banReason.ifBlank { "Not specified" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Banned at: $bannedAt", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Submit Appeal", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = appealMessage,
                onValueChange = { appealMessage = it },
                placeholder = { Text("Explain why this ban should be reviewed...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (uid.isBlank()) return@Button
                    scope.launch {
                        isSubmitting = true
                        feedback = ""
                        val result = appealRepo.createAppeal(uid, appealMessage)
                        result.onSuccess {
                            appealMessage = ""
                            feedback = "Appeal submitted."
                            refresh()
                        }
                        result.onFailure { e ->
                            feedback = e.message ?: "Unable to submit appeal."
                        }
                        isSubmitting = false
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Appeal")
            }

            if (feedback.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(feedback, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }

            Spacer(Modifier.height(16.dp))

            Text("Appeal History", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            if (appeals.isEmpty()) {
                Text("No appeals submitted yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(appeals) { appeal ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Status: ${appeal.status}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(appeal.message, style = MaterialTheme.typography.bodySmall)
                                if (appeal.reviewNotes.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Admin notes: ${appeal.reviewNotes}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
            ) {
                Text("Check Status")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        AuthRepository(context).logout()
                        nav.navigate("login_choice") {
                            popUpTo("dashboard_router") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
            ) {
                Text("Log Out")
            }
        }
    }
}
