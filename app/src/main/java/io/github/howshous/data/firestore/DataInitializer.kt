package io.github.howshous.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DataInitializer {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Creates sample notifications for testing
     */
    suspend fun createSampleNotifications(userId: String) {
        try {
            val notifications = listOf(
                mapOf(
                    "userId" to userId,
                    "type" to "payment_due",
                    "title" to "Rent Due Tomorrow",
                    "message" to "Your monthly rent is due tomorrow. Please ensure timely payment.",
                    "read" to false,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "actionUrl" to ""
                ),
                mapOf(
                    "userId" to userId,
                    "type" to "message",
                    "title" to "New Message",
                    "message" to "You have a new message from your landlord.",
                    "read" to false,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "actionUrl" to ""
                )
            )

            for (notif in notifications) {
                db.collection("notifications").add(notif).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Creates sample listings for testing
     */
    suspend fun createSampleListings(landlordId: String) {
        try {
            val listings = listOf(
                mapOf(
                    "landlordId" to landlordId,
                    "title" to "Cozy Apartment in Downtown",
                    "description" to "Beautiful apartment with modern amenities",
                    "location" to "Downtown",
                    "price" to 25000,
                    "deposit" to 50000,
                    "status" to "active",
                    "photos" to listOf("https://via.placeholder.com/300x200?text=Apartment+1"),
                    "amenities" to listOf("WiFi", "Parking", "Kitchen"),
                    "createdAt" to com.google.firebase.Timestamp.now()
                ),
                mapOf(
                    "landlordId" to landlordId,
                    "title" to "Spacious House with Garden",
                    "description" to "Large house with beautiful garden and outdoor space",
                    "location" to "Suburbs",
                    "price" to 45000,
                    "deposit" to 90000,
                    "status" to "active",
                    "photos" to listOf("https://via.placeholder.com/300x200?text=House+1"),
                    "amenities" to listOf("Garden", "Garage", "Backyard"),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
            )

            for (listing in listings) {
                db.collection("listings").add(listing).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
