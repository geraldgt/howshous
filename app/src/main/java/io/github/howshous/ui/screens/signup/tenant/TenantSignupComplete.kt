package io.github.howshous.ui.screens.signup.tenant

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.viewmodels.SignupViewModel
import kotlinx.coroutines.launch

@Composable
fun TenantSignupComplete(nav: NavController, signupVM: SignupViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TenantGreen)
            .padding(32.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            // Back
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        painter = painterResource(R.drawable.i_back),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Logo
            Image(
                painter = painterResource(R.drawable.logo_white),
                contentDescription = null,
                modifier = Modifier.size(150.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                "Tenant",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.spr_tenant_all_done),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(24.dp))

            // “You are all set up!”
            Text(
                "You are all set up!",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Secondary message
            Text(
                "Verification may take some time but\nyou can immediately login.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    scope.launch {
                        signupVM.finishTenantSignup(
                            context = context,
                            nav = nav
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = TenantGreen
                )
            ) {
                Text("Login")
            }
        }
    }
}