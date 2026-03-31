import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import io.github.howshous.ui.screens.shells.LandlordMainShell
import io.github.howshous.ui.screens.shells.TenantMainShell
import io.github.howshous.ui.screens.shells.AdminMainShell

@Composable
fun DashboardRouter(
    nav: NavHostController,
    role: String
) {
    when (role) {
        "tenant" -> TenantMainShell(nav)
        "landlord" -> LandlordMainShell(nav)
        "administrator" -> AdminMainShell(nav)
        "banned" -> BannedAccountScreen(nav)
        else -> {
            // Show loading spinner while role is being loaded, or if role is invalid
            if (role.isBlank()) {
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
