package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDetailScreen(nav: NavController, chatId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        if (chatId.isNotEmpty()) {
            viewModel.loadMessagesForChat(chatId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3EDF7))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Conversation", style = MaterialTheme.typography.titleMedium)
        }

        // Messages
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Text(
                    "No messages yet. Start the conversation!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message ->
                        val isUserMessage = message.senderId == uid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 250.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserMessage) Color(0xFF1BA37C) else Color(0xFFFFFFFF)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        message.text,
                                        color = if (isUserMessage) Color.White else Color.Black,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        formatTime(message.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUserMessage) Color.White.copy(alpha = 0.7f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && uid.isNotEmpty()) {
                        viewModel.sendMessage(chatId, uid, messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFF1BA37C))
            }
        }
    }
}

private fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
    return if (timestamp != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp.seconds * 1000))
    } else {
        ""
    }
}
