package io.github.howshous.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.models.UserProfile
import io.github.howshous.data.models.Listing
import io.github.howshous.data.models.Chat
import io.github.howshous.data.models.Message
import io.github.howshous.data.models.Notification
import io.github.howshous.data.models.Rental
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        try {
            db.collection("users").document(uid).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllListings(): List<Listing> {
        return try {
            val snap = db.collection("listings").get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListingsByLocation(location: String): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("location", location)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListingsForLandlord(landlordId: String): List<Listing> {
        return try {
            val snap = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getListing(id: String): Listing? {
        return try {
            val doc = db.collection("listings").document(id).get().await()
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createListing(listing: Listing): String {
        return try {
            val ref = db.collection("listings").add(listing.copy(id = "")).await()
            ref.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getChatsForUser(userId: String): List<Chat> {
        return try {
            val snap = db.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Chat::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMessagesForChat(chatId: String): List<Message> {
        return try {
            val snap = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        try {
            val message = Message(
                senderId = senderId,
                text = text,
                chatId = chatId,
                timestamp = com.google.firebase.Timestamp.now()
            )
            db.collection("chats").document(chatId).collection("messages").add(message).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createChat(chat: Chat): String {
        return try {
            val docRef = db.collection("chats").add(chat).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getNotificationsForUser(userId: String, limit: Long = 20): List<Notification> {
        return try {
            val snap = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update("read", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class RentalRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRentalsForTenant(tenantId: String): List<Rental> {
        return try {
            val snap = db.collection("rentals")
                .whereEqualTo("tenantId", tenantId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Rental::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRentalsForLandlord(landlordId: String): List<Rental> {
        return try {
            val snap = db.collection("rentals")
                .whereEqualTo("landlordId", landlordId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(Rental::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
