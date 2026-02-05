package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.theme.LandlordBlue
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.DueText
import io.github.howshous.ui.theme.slightlyGray
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.geometry.Offset

@Composable
fun ChatDetailScreen(nav: NavController, chatId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showContractDialog by remember { mutableStateOf<String?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listingRepository = remember { ListingRepository() }
    var listing by remember { mutableStateOf<io.github.howshous.data.models.Listing?>(null) }

    LaunchedEffect(chatId, uid) {
        if (chatId.isNotEmpty() && uid.isNotEmpty()) {
            viewModel.loadMessagesForChat(chatId, uid)
        }
    }

    LaunchedEffect(currentChat) {
        currentChat?.listingId?.let { listingId ->
            if (listingId.isNotEmpty()) {
                listing = listingRepository.getListing(listingId)
            }
        }
    }

    val isLandlord = currentChat?.landlordId == uid
    val userAccentColor = if (isLandlord) LandlordBlue else TenantGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
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
                    // Show contracts
                    items(contracts.reversed()) { contract ->
                        ContractMessageCard(
                            contract = contract,
                            isLandlord = isLandlord,
                            onViewContract = { showContractDialog = contract.id },
                            onSignContract = {
                                scope.launch {
                                    viewModel.signContract(contract.id)
                                }
                            }
                        )
                    }
                    // Show messages
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
                                    containerColor = if (isUserMessage) userAccentColor else NearWhite
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (message.type == "image" && message.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = message.imageUrl,
                                            contentDescription = message.text.ifBlank { "Image" },
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clickable {
                                                    selectedImageUrl = message.imageUrl
                                                    showImageViewer = true
                                                }
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    if (message.text.isNotBlank()) {
                                        Text(
                                            message.text,
                                            color = if (isUserMessage) Color.White else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        formatTime(message.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUserMessage) Color.White.copy(alpha = 0.7f) else slightlyGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contract button for landlords
        if (isLandlord && listing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.sendContract(
                                chatId = chatId,
                                listingId = listing!!.id,
                                landlordId = uid,
                                tenantId = currentChat?.tenantId ?: "",
                                monthlyRent = listing!!.price,
                                deposit = listing!!.deposit
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LandlordBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("📄 Send Contract")
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
                shape = InputShape,
                colors = inputColors(),
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
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = userAccentColor)
            }
        }
    }

    // Contract dialog
    showContractDialog?.let { contractId ->
        val contract = contracts.find { it.id == contractId }
        contract?.let {
            ContractDialog(
                contract = it,
                isLandlord = isLandlord,
                onDismiss = { showContractDialog = null },
                onSign = {
                    scope.launch {
                        viewModel.signContract(it.id)
                        showContractDialog = null
                    }
                }
            )
        }
    }

    if (showImageViewer && selectedImageUrl.isNotBlank()) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            offset += panChange
        }

        Dialog(onDismissRequest = { showImageViewer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Full image view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(transformableState)
                )
                IconButton(
                    onClick = { showImageViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ContractMessageCard(
    contract: io.github.howshous.data.models.Contract,
    isLandlord: Boolean,
    onViewContract: () -> Unit,
    onSignContract: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onViewContract() },
        colors = CardDefaults.cardColors(
            containerColor = NearWhite
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📄", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        contract.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = LandlordBlue
                    )
                }
                Text(
                    contract.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (contract.status) {
                        "signed" -> PricePointGreen
                        "pending" -> DueText
                        "terminated" -> Color.Red
                        else -> slightlyGray
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Rent: ₱${contract.monthlyRent}/month", style = MaterialTheme.typography.bodySmall)
            Text("Deposit: ₱${contract.deposit}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if (!isLandlord && contract.status == "pending") {
                Button(
                    onClick = onSignContract,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TenantGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Agree to Contract")
                }
            } else {
                TextButton(
                    onClick = onViewContract,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Details")
                }
            }
        }
    }
}

@Composable
fun ContractDialog(
    contract: io.github.howshous.data.models.Contract,
    isLandlord: Boolean,
    onDismiss: () -> Unit,
    onSign: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contract.title) },
        text = {
            Column {
                Text("Monthly Rent: ₱${contract.monthlyRent}", style = MaterialTheme.typography.bodyMedium)
                Text("Deposit: ₱${contract.deposit}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("Terms and Conditions:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(contract.terms, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (!isLandlord && contract.status == "pending") {
                Button(onClick = onSign) {
                    Text("Agree to Contract")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!isLandlord && contract.status == "pending") {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
    return if (timestamp != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp.seconds * 1000))
    } else {
        ""
    }
}
