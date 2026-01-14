package io.github.howshous.data.rental

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.Timestamp

class RentalRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createRentalFromContract(contractId: String) {
        val contractDoc = db.collection("contracts").document(contractId).get().await()
        val listingId = contractDoc.getString("listingId")!!
        val tenantId = contractDoc.getString("tenantId")!!
        val landlordId = contractDoc.getString("landlordId")!!
        val rent = contractDoc.getLong("price")?.toInt() ?: 0

        val rentalId = UUID.randomUUID().toString()
        val rental = hashMapOf(
            "listingId" to listingId,
            "tenantId" to tenantId,
            "landlordId" to landlordId,
            "contractId" to contractId,
            "startDate" to Timestamp.now(),
            "nextDueDate" to Timestamp.now(), // compute real next due date from contractData.payment_day
            "status" to "active"
        )
        db.collection("rentals").document(rentalId).set(rental).await()

        // Optionally set listing status to full
        db.collection("listings").document(listingId).update("status", "full").await()
    }
}
