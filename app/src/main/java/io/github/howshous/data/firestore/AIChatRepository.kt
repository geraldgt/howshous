package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AIChatMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val isTenant: Boolean = true, // true for tenant, false for AI
    val timestamp: Timestamp? = null
)

class AIChatRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Document id for the chat. Use null for default (tenant) chat; use "landlord_analytics" for landlord analytics chat.
     */
    private fun docId(userId: String, chatKey: String?): String =
        if (chatKey.isNullOrBlank()) userId else "${chatKey}_$userId"

    suspend fun saveMessage(userId: String, text: String, isTenant: Boolean, chatKey: String? = null) {
        try {
            val message = hashMapOf(
                "userId" to userId,
                "text" to text,
                "isTenant" to isTenant,
                "timestamp" to Timestamp.now()
            )
            db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .add(message)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadMessages(userId: String, chatKey: String? = null): List<AIChatMessage> {
        return try {
            val snap = db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .orderBy("timestamp")
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                AIChatMessage(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    text = doc.getString("text") ?: "",
                    isTenant = doc.getBoolean("isTenant") ?: true,
                    timestamp = doc.getTimestamp("timestamp")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Deletes all messages in a chat (e.g. landlord_analytics). Used for "Clear chat".
     */
    suspend fun deleteChatHistory(userId: String, chatKey: String? = null) {
        try {
            val snap = db.collection("aiChats")
                .document(docId(userId, chatKey))
                .collection("messages")
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun initializeWelcomeMessage(userId: String) {
        // Check if user already has messages
        val existingMessages = loadMessages(userId)
        if (existingMessages.isEmpty()) {
            // Add welcome message if this is the first time
            saveMessage(
                userId = userId,
                text = "Hi! I'm your boarding house assistant for Baguio City. Tell me your budget, preferred location in Baguio, and must-have amenities and I'll point you to the best matches.",
                isTenant = false
            )
        }
    }

    suspend fun initializeLandlordAnalyticsWelcome(userId: String) {
        val existingMessages = loadMessages(userId, "landlord_analytics")
        if (existingMessages.isEmpty()) {
            saveMessage(
                userId = userId,
                text = "Insights based on your recent activity. Ask about your views, saves, and conversion rates—I’ll explain what the numbers suggest and possible improvements (not guarantees).",
                isTenant = false,
                chatKey = "landlord_analytics"
            )
        }
    }
}

