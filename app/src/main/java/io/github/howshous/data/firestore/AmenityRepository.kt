package io.github.howshous.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.catalog.DefaultAmenities
import kotlinx.coroutines.tasks.await

class AmenityRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAvailableAmenities(): List<String> {
        val custom = runCatching {
            db.collection("amenity_tags")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.getString("label")?.let(DefaultAmenities::normalizeLabel)
                }
        }.getOrElse {
            it.printStackTrace()
            emptyList()
        }

        return (DefaultAmenities.labels + custom)
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    suspend fun createAmenity(label: String, createdBy: String): Result<String> {
        val normalized = DefaultAmenities.normalizeLabel(label)
        if (normalized.length < 2) {
            return Result.failure(IllegalArgumentException("Amenity name is too short."))
        }
        if (normalized.length > 40) {
            return Result.failure(IllegalArgumentException("Amenity name is too long."))
        }

        val existing = getAvailableAmenities()
        if (existing.any { it.equals(normalized, ignoreCase = true) }) {
            return Result.success(existing.first { it.equals(normalized, ignoreCase = true) })
        }

        return try {
            db.collection("amenity_tags")
                .add(
                    mapOf(
                        "label" to normalized,
                        "createdBy" to createdBy,
                        "createdAt" to Timestamp.now()
                    )
                )
                .await()
            Result.success(normalized)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
