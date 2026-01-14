package io.github.howshous.data.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.firestore.ListenerRegistration

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getOrCreateChat(listingId: String, tenantId: String, landlordId: String): String {
        // Try to find an existing chat for (listingId, tenantId)
        val q = db.collection("chats")
            .whereEqualTo("listingId", listingId)
            .whereEqualTo("tenantId", tenantId)
            .limit(1)
            .get()
            .await()
        if (!q.isEmpty) return q.documents[0].id

        val chatId = UUID.randomUUID().toString()
        val chatDoc = mapOf(
            "listingId" to listingId,
            "tenantId" to tenantId,
            "landlordId" to landlordId,
            "lastMessage" to "",
            "lastTimestamp" to Timestamp.now()
        )
        db.collection("chats").document(chatId).set(chatDoc).await()
        return chatId
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        val msgId = UUID.randomUUID().toString()
        val msg = mapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to Timestamp.now()
        )
        db.collection("chatMessages").document(chatId).collection("messages").document(msgId).set(msg).await()
        db.collection("chats").document(chatId).update(mapOf("lastMessage" to text, "lastTimestamp" to Timestamp.now())).await()
    }

    // Real time listener helper - returns ListenerRegistration for removal
    fun listenMessages(chatId: String, onChange: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        return db.collection("chatMessages").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val msgs = snap.documents.map { it.data ?: mapOf<String, Any>() }
                onChange(msgs)
            }
    }
}
