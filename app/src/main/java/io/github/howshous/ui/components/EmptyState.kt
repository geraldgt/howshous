package io.github.howshous.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        if (actionButton != null) {
            Spacer(Modifier.height(24.dp))
            actionButton()
        }
    }
}

@Composable
fun NoChatsEmptyState(modifier: Modifier = Modifier, onStartChat: (() -> Unit)? = null) {
    EmptyState(
        icon = Icons.Outlined.MailOutline,
        title = "No Conversations",
        subtitle = "Start a conversation by contacting a property owner",
        modifier = modifier,
        actionButton = if (onStartChat != null) {
            { Button(onClick = onStartChat) { Text("Browse Listings") } }
        } else null
    )
}

@Composable
fun NoListingsEmptyState(modifier: Modifier = Modifier, onCreateListing: (() -> Unit)? = null) {
    EmptyState(
        icon = Icons.Outlined.Home,
        title = "No Listings",
        subtitle = "Create your first listing to get started",
        modifier = modifier,
        actionButton = if (onCreateListing != null) {
            { Button(onClick = onCreateListing) { Text("Create Listing") } }
        } else null
    )
}

@Composable
fun NoNotificationsEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Notifications,
        title = "No Notifications",
        subtitle = "You're all caught up!"
    )
}

@Composable
fun ErrorState(
    error: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Something went wrong",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Try Again") }
        }
    }
}
