package io.github.howshous.data.listing

import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.util.UUID

data class Listing(
    val id: String = "",
    val landlordId: String,
    val title: String,
    val description: String,
    val price: Int,
    val deposit: Int = 0,
    val status: String = "active",
    val location: String = "",
    val photos: List<String> = listOf(),
    val createdAt: Timestamp = Timestamp.now(),
    val contractTemplate: Map<String, Any>? = null // optional
)

class ListingRepository {
    private val db = Firebase.firestore

    suspend fun createListing(listing: Listing): String {
        val id = if (listing.id.isBlank()) UUID.randomUUID().toString() else listing.id
        val map = hashMapOf<String, Any>(
            "landlordId" to listing.landlordId,
            "title" to listing.title,
            "description" to listing.description,
            "price" to listing.price,
            "deposit" to listing.deposit,
            "status" to listing.status,
            "location" to listing.location,
            "photos" to listing.photos,
            "createdAt" to listing.createdAt
        )
        if (listing.contractTemplate != null) map["contractTemplate"] = listing.contractTemplate
        db.collection("listings").document(id).set(map).await()
        return id
    }

    suspend fun getListing(id: String) = db.collection("listings").document(id).get().await()

    suspend fun getListingsForLandlord(landlordId: String) =
        db.collection("listings").whereEqualTo("landlordId", landlordId).get().await()

    suspend fun updateListing(id: String, updates: Map<String, Any>) {
        db.collection("listings").document(id).update(updates).await()
    }
}