package io.github.howshous.data.contracts

import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.Timestamp

class ContractRepository {
    private val db = Firebase.firestore

    suspend fun createFromTemplate(listingId: String, chatId: String, tenantId: String, landlordId: String): String {
        // Read listing and contract template
        val listingDoc = db.collection("listings").document(listingId).get().await()
        val template = listingDoc.get("contractTemplate") as? Map<String, Any>
        val id = UUID.randomUUID().toString()
        val contractData = hashMapOf<String, Any>(
            "listingId" to listingId,
            "chatId" to chatId,
            "tenantId" to tenantId,
            "landlordId" to landlordId,
            "status" to "pending",
            "createdAt" to Timestamp.now()
        )
        // copy template fields into the instance
        template?.forEach { (k, v) -> contractData[k] = v }
        db.collection("contracts").document(id).set(contractData).await()
        return id
    }

    suspend fun signContract(contractId: String, tenantId: String) {
        db.collection("contracts").document(contractId)
            .update(mapOf("status" to "signed", "signedAt" to Timestamp.now(), "signedBy" to tenantId)).await()
    }

    suspend fun confirmContract(contractId: String) {
        db.collection("contracts").document(contractId).update(mapOf("status" to "confirmed")).await()
    }
}