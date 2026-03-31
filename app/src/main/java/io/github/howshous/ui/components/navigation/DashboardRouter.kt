import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.screens.shells.LandlordMainShell
import io.github.howshous.ui.screens.shells.TenantMainShell
import io.github.howshous.ui.screens.shells.AdminMainShell
import io.github.howshous.ui.data.saveRole
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.tasks.await

@Composable
fun DashboardRouter(
    nav: NavHostController,
    role: String
) {
    var freshRole by remember { mutableStateOf(role) }
    var isLoadingFreshRole by remember { mutableStateOf(true) }
    var refreshAttempts by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = readUidFlow(context).collectAsState(initial = "").value
    
    // Fetch the latest role from Firestore to catch real-time bans
    LaunchedEffect(uid) {
        // Only attempt refresh if uid changes, limit refresh attempts to prevent loops
        if (uid.isNotBlank() && refreshAttempts < 3) {
            refreshAttempts += 1
            scope.launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(uid).get().await()
                    val firestoreRole = userDoc.getString("role") ?: ""
                    if (firestoreRole.isNotBlank()) {
                        freshRole = firestoreRole
                        // Update local cache if it's different
                        if (firestoreRole != role) {
                            saveRole(context, firestoreRole)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If fetch fails, use cached role
                    freshRole = role
                } finally {
                    isLoadingFreshRole = false
                }
            }
        } else {
            isLoadingFreshRole = false
        }
    }
    
    // Show loading while fetching fresh role
    if (isLoadingFreshRole) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    when (freshRole) {
        "tenant" -> TenantMainShell(nav)
        "landlord" -> LandlordMainShell(nav)
        "administrator" -> AdminMainShell(nav)
        "banned" -> BannedAccountScreen(nav)
        else -> {
            // Show loading spinner while role is being loaded, or if role is invalid
            if (freshRole.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
