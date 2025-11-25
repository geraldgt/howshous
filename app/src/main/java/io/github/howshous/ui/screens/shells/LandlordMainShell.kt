package io.github.howshous.ui.screens.shells

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.howshous.R
import io.github.howshous.ui.components.BottomNavBar
import io.github.howshous.ui.components.BottomNavItem
import io.github.howshous.ui.components.TopBar
import io.github.howshous.ui.screens.main_landlord.*

@Composable
fun LandlordMainShell(rootNav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val innerNav = rememberNavController()
    var selectedRoute by remember { mutableStateOf("landlord_home") }

    val bottomNavItems = listOf(
        BottomNavItem("Home", R.drawable.i_home_0, R.drawable.i_home_1, "landlord_home"),
        BottomNavItem("Listings", R.drawable.i_list_0, R.drawable.i_list_1, "landlord_listings"),
        BottomNavItem("Contact", R.drawable.i_message_0, R.drawable.i_message_1, "landlord_contact"),
        BottomNavItem("Alerts", R.drawable.i_bell_0, R.drawable.i_bell_1, "landlord_notifications"),
        BottomNavItem("Account", R.drawable.i_account_0, R.drawable.i_account_1, "landlord_account")
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopBar(
            role = "landlord",
            onSettingsClick = {
                rootNav.navigate("settings")
            }
        )

        // Body - Inner NavHost
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavHost(
                navController = innerNav,
                startDestination = "landlord_home"
            ) {
                composable("landlord_home") {
                    selectedRoute = "landlord_home"
                    LandlordHome(rootNav)
                }
                composable("landlord_listings") {
                    selectedRoute = "landlord_listings"
                    LandlordListings(rootNav)
                }
                composable("landlord_contact") {
                    selectedRoute = "landlord_contact"
                    LandlordChatList(rootNav)
                }
                composable("landlord_notifications") {
                    selectedRoute = "landlord_notifications"
                    LandlordNotifications(rootNav)
                }
                composable("landlord_account") {
                    selectedRoute = "landlord_account"
                    LandlordAccount(rootNav)
                }
            }
        }

        // Bottom Nav Bar
        BottomNavBar(
            items = bottomNavItems,
            selectedRoute = selectedRoute,
            onItemClick = { route ->
                selectedRoute = route
                innerNav.navigate(route) {
                    popUpTo("landlord_home") { inclusive = false }
                    launchSingleTop = true
                }
            },
            role = "landlord"
        )
    }
}
