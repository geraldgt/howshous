package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.data.readUidFlow
import kotlinx.coroutines.launch

@Composable
fun InitiateChatScreen(nav: NavController, listingId: String = "", landlordId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attachIdCard by remember { mutableStateOf(false) }
    var idCardUrl by remember { mutableStateOf("") }
    var isFirstContact by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(uid, listingId, landlordId) {
        if (uid.isBlank()) return@LaunchedEffect
        val chatRepository = ChatRepository()
        val existingChatId = chatRepository.getExistingChatIdForListing(
            listingId = listingId,
            tenantId = uid,
            landlordId = landlordId
        )
        isFirstContact = existingChatId.isBlank() || !chatRepository.hasMessages(existingChatId)

        if (isFirstContact) {
            val userRepository = UserRepository()
            idCardUrl = userRepository.getVerificationIdUrl(uid)
        } else {
            idCardUrl = ""
            attachIdCard = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Start Conversation", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Send a message to the landlord", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    errorMessage = ""
                },
                label = { Text("Your Message") },
                shape = InputShape,
                colors = inputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(16.dp))
            if (isFirstContact) {
                Text("Optional: Attach your ID card", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = attachIdCard,
                        onCheckedChange = { attachIdCard = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Attach ID?")
                }

                if (attachIdCard) {
                    if (idCardUrl.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = idCardUrl,
                            contentDescription = "ID card preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No ID on file. Please upload a valid ID in your profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (uid.isEmpty()) {
                        errorMessage = "User not authenticated"
                        return@Button
                    }
                    if (message.isEmpty()) {
                        errorMessage = "Message cannot be empty"
                        return@Button
                    }
                    if (isFirstContact && attachIdCard && idCardUrl.isBlank()) {
                        errorMessage = "No ID on file. Please upload a valid ID in your profile."
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val chatRepository = ChatRepository()
                            val chatId = chatRepository.getOrCreateChatForListing(
                                listingId = listingId,
                                tenantId = uid,
                                landlordId = landlordId
                            )
                            if (chatId.isNotEmpty()) {
                                chatRepository.sendMessage(chatId, uid, message)
                                if (isFirstContact && attachIdCard && idCardUrl.isNotBlank()) {
                                    chatRepository.sendImageMessage(
                                        chatId = chatId,
                                        senderId = uid,
                                        imageUrl = idCardUrl,
                                        label = "ID Card"
                                    )
                                }
                            }

                            if (chatId.isNotEmpty()) {
                                isLoading = false
                                nav.navigate("chat/$chatId") {
                                    popUpTo("initiate_chat/$listingId/$landlordId") { inclusive = true }
                                }
                            } else {
                                errorMessage = "Failed to create conversation"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to start conversation: ${e.message}"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = message.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Message")
                }
            }
        }
    }
}
