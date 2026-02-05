package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.R
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.data.models.UserProfile
import androidx.compose.ui.geometry.Offset
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.ensureSessionId
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ListingViewModel

@Composable
fun ListingDetailScreen(nav: NavController, listingId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val role by readRoleFlow(context).collectAsState(initial = "")
    val viewModel: ListingViewModel = viewModel()
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    var sessionId by remember { mutableStateOf<String?>(null) }
    var landlordProfile by remember { mutableStateOf<UserProfile?>(null) }
    val userRepository = remember { UserRepository() }
    var showLandDeedViewer by remember { mutableStateOf(false) }

    LaunchedEffect(listingId, uid) {
        viewModel.loadListing(listingId)
        if (uid.isNotBlank()) {
            viewModel.loadSavedState(listingId, uid)
        }
    }

    LaunchedEffect(listing) {
        val landlordId = listing?.landlordId ?: ""
        if (landlordId.isNotBlank()) {
            landlordProfile = userRepository.getUserProfile(landlordId)
        } else {
            landlordProfile = null
        }
    }

    LaunchedEffect(Unit) {
        sessionId = ensureSessionId(context)
    }

    LaunchedEffect(listingId, uid, role, listing, sessionId) {
        val currentListing = listing
        val currentSessionId = sessionId
        if (role == "tenant" && uid.isNotBlank() && currentListing != null && currentSessionId != null) {
            // Only count as a view if the detail screen stays visible for a short time window.
            // This helps avoid counting very quick scroll-past or accidental opens.
            kotlinx.coroutines.delay(2500)
            viewModel.recordUniqueViewWithAnalytics(
                listingId = listingId,
                viewerId = uid,
                sessionId = currentSessionId,
                price = currentListing.price
            )
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Listing Details", style = MaterialTheme.typography.titleMedium)
            }
            if (role == "tenant" && listing != null && uid.isNotBlank()) {
                IconButton(
                    onClick = {
                        viewModel.toggleSave(
                            listingId = listingId,
                            userId = uid,
                            sessionId = sessionId
                        )
                    }
                ) {
                    if (isSaved) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Unsave",
                            tint = PricePointGreen
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save"
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else if (listing != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Photos
                item {
                    if (listing!!.photos.isNotEmpty()) {
                        val listState = rememberLazyListState()
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val itemWidth = maxWidth
                            LazyRow(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(listing!!.photos) { _, photoUrl ->
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = listing!!.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .height(200.dp)
                                    )
                                }
                            }
                            if (listing!!.photos.size > 1) {
                                val currentIndex = listState.firstVisibleItemIndex
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(listing!!.photos.size) { index ->
                                        val color = if (index == currentIndex) PricePointGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color, shape = MaterialTheme.shapes.small)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Title
                item {
                    Text(listing!!.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                }

                // Location & Price
                item {
                    Text(listing!!.location, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("₱${listing!!.price}/month", style = MaterialTheme.typography.titleMedium, color = PricePointGreen)
                    Text("Deposit: ₱${listing!!.deposit}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (role == "tenant") {
                        val landlordName = listOfNotNull(
                            landlordProfile?.firstName?.takeIf { it.isNotBlank() },
                            landlordProfile?.lastName?.takeIf { it.isNotBlank() }
                        ).joinToString(" ").ifBlank { "Landlord" }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Landlord:", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                landlordName,
                                style = MaterialTheme.typography.bodySmall,
                                color = PricePointGreen,
                                modifier = Modifier.clickable {
                                    val landlordId = listing!!.landlordId
                                    if (landlordId.isNotBlank()) {
                                        nav.navigate("landlord_profile/$landlordId")
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (role == "landlord") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.i_eyeopen),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${listing!!.uniqueViewCount} unique views",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Description
                item {
                    Text("Description", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(listing!!.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                }

                // Amenities
                item {
                    if (listing!!.amenities.isNotEmpty()) {
                        Text("Amenities", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        listing!!.amenities.forEach { amenity ->
                            Text("• $amenity", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Land Deed (tenant view)
                item {
                    if (role == "tenant") {
                        Text("Land Deed", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        if (listing!!.landDeedUrl.isNotBlank()) {
                            AsyncImage(
                                model = listing!!.landDeedUrl,
                                contentDescription = "Land deed",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { showLandDeedViewer = true }
                            )
                        } else {
                            Text(
                                "Land deed not provided.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Contact Button
                item {
                    Button(
                        onClick = {
                            nav.navigate("initiate_chat/${listingId}/${listing!!.landlordId}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Contact Landlord")
                    }
                }
            }
        } else {
                    Text(
                        "Listing not found",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(32.dp)
                    )
        }
    }

    if (showLandDeedViewer && listing != null && listing!!.landDeedUrl.isNotBlank()) {
        val deedUrl = listing!!.landDeedUrl
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            offset += panChange
        }

        Dialog(onDismissRequest = { showLandDeedViewer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = deedUrl,
                    contentDescription = "Land deed full view",
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
                    onClick = { showLandDeedViewer = false },
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
