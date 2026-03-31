package io.github.howshous.ui.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

object SampleListingsGenerator {

    private data class SampleListingTemplate(
        val title: String,
        val description: String,
        val location: String,
        val price: Int,
        val deposit: Int,
        val capacity: Int,
        val status: String,
        val amenities: List<String>,
        val photos: List<String> = emptyList()
    )

    private val sampleListings = listOf(
        SampleListingTemplate(
            title = "Cozy Studio Near Session Road",
            description = "Bright and airy studio unit perfect for students or young professionals. Walking distance to Session Road, SM Baguio, and restaurants. Fully furnished with basic appliances.",
            location = "Session Road",
            price = 4500,
            deposit = 9000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("WiFi", "Air Conditioning", "Furnished", "Near Public Transport", "Security")
        ),
        SampleListingTemplate(
            title = "Spacious Room with Private Bathroom",
            description = "Large room with private bathroom in a quiet neighborhood. Shared kitchen and living area. Perfect for professionals working in Baguio City Center.",
            location = "Baguio City Center",
            price = 8000,
            deposit = 16000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "CCTV", "Security")
        ),
        SampleListingTemplate(
            title = "Budget-Friendly Boarding House",
            description = "Affordable room in a friendly boarding house. Shared facilities, clean and well-maintained. Great for students on a tight budget.",
            location = "Irisan",
            price = 3000,
            deposit = 6000,
            capacity = 4,
            status = "under_review",
            amenities = listOf("WiFi", "Kitchen Access", "Laundry", "Security")
        ),
        SampleListingTemplate(
            title = "Premium Room with Gym Access",
            description = "Luxury boarding house with modern amenities. Access to gym and swimming pool. Perfect for professionals who value comfort and convenience.",
            location = "Camp John Hay",
            price = 12000,
            deposit = 24000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        ),
        SampleListingTemplate(
            title = "Pet-Friendly Boarding House",
            description = "Comfortable room in a pet-friendly environment. Your furry friends are welcome! Close to Burnham Park and pet stores.",
            location = "Burnham Park",
            price = 6000,
            deposit = 12000,
            capacity = 2,
            status = "under_review",
            amenities = listOf("Pets Allowed", "Free Parking", "WiFi", "Air Conditioning", "Kitchen Access", "Laundry", "Security")
        ),
        SampleListingTemplate(
            title = "Student-Friendly Dormitory",
            description = "Clean and safe dormitory near universities. Study areas available. Strict security for peace of mind.",
            location = "Legarda Road",
            price = 3500,
            deposit = 7000,
            capacity = 6,
            status = "under_review",
            amenities = listOf("WiFi", "Air Conditioning", "Security", "CCTV", "Near Public Transport")
        ),
        SampleListingTemplate(
            title = "Modern Studio with Balcony",
            description = "Newly renovated studio with private balcony. Modern design, fully furnished. Perfect for young professionals.",
            location = "Aurora Hill",
            price = 9500,
            deposit = 19000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("WiFi", "Air Conditioning", "Furnished", "Kitchen Access", "Laundry", "Security", "CCTV")
        ),
        SampleListingTemplate(
            title = "Affordable Room Near Business District",
            description = "Simple but comfortable room near major business areas. Easy commute to Session Road and SM Baguio.",
            location = "Lower Magsaysay",
            price = 5000,
            deposit = 10000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("WiFi", "Air Conditioning", "Near Public Transport", "Security", "Kitchen Access")
        ),
        SampleListingTemplate(
            title = "Family-Style Boarding House",
            description = "Homey atmosphere in a family-run boarding house. Shared meals available. Great for those looking for a community feel.",
            location = "Quezon Hill",
            price = 4000,
            deposit = 8000,
            capacity = 3,
            status = "under_review",
            amenities = listOf("WiFi", "Kitchen Access", "Laundry", "Security", "Near Public Transport")
        ),
        SampleListingTemplate(
            title = "Luxury Room with Mountain View",
            description = "Premium room with stunning mountain views. Top-of-the-line amenities and 24/7 security. Perfect for those who want the best.",
            location = "Mines View Park",
            price = 15000,
            deposit = 30000,
            capacity = 1,
            status = "under_review",
            amenities = listOf("Free Parking", "WiFi", "Air Conditioning", "Gym Access", "Swimming Pool", "Furnished", "Security", "CCTV", "Kitchen Access", "Laundry")
        )
    )

    private val reviewCounts = listOf(
        48 to 2,  // 96% overwhelmingly positive
        18 to 6,  // 75% mostly positive
        10 to 12, // 45% mixed
        30 to 3,  // 91% very positive
        14 to 9,  // 61% mixed
        8 to 14,  // 36% mostly negative
        22 to 4,  // 85% positive
        12 to 3,  // 80% positive
        6 to 9,   // 40% mixed
        38 to 1   // 97% overwhelmingly positive
    )

    private val positiveComments = listOf(
        "Clean, quiet, and exactly as described.",
        "Great value for the price. Would recommend.",
        "Owner was responsive and helpful.",
        "Comfortable stay and solid amenities.",
        "Felt safe and convenient for work."
    )

    private val negativeComments = listOf(
        "Needs better maintenance in common areas.",
        "Not as quiet as expected during weekends.",
        "Photos looked better than the actual room.",
        "WiFi was unstable at times.",
        "Security could be improved."
    )
    
    suspend fun generateSampleListings(
        landlordId: String,
        titlePrefix: String? = null
    ): List<String> {
        if (landlordId.isBlank()) return emptyList()
        val db = FirebaseFirestore.getInstance()
        val createdIds = mutableListOf<String>()
        val normalizedPrefix = titlePrefix?.trim()?.takeIf { it.isNotBlank() }

        // First, delete existing sample listings for this landlord
        deleteExistingSampleListings(landlordId)

        // Then create new sample listings
        sampleListings.forEachIndexed { index, template ->
            val docRef = db.collection("listings").document()
            val now = Timestamp.now()
            val finalTitle = if (normalizedPrefix != null) {
                "$normalizedPrefix ${template.title}"
            } else {
                template.title
            }
            val listingMap = hashMapOf<String, Any>(
                "landlordId" to landlordId,
                "title" to finalTitle,
                "description" to template.description,
                "location" to template.location,
                "price" to template.price,
                "deposit" to template.deposit,
                "capacity" to template.capacity,
                "status" to template.status,
                "previousStatus" to "",
                "reviewedBy" to "",
                "reviewNotes" to "",
                "amenities" to template.amenities,
                "photos" to template.photos,
                "createdAt" to now,
                "updatedAt" to now,
                "uniqueViewCount" to 0,
                "isSample" to true,
                "reviewSummary" to mapOf(
                    "total" to 0,
                    "recommendedCount" to 0,
                    "notRecommendedCount" to 0,
                    "updatedAt" to now
                )
            )
            docRef.set(listingMap).await()
            createdIds.add(docRef.id)
            val (recommendedCount, notRecommendedCount) = reviewCounts.getOrElse(index) { 0 to 0 }
            seedSampleReviewsForListing(
                listingId = docRef.id,
                recommendedCount = recommendedCount,
                notRecommendedCount = notRecommendedCount
            )
        }
        return createdIds
    }
    
    private suspend fun deleteExistingSampleListings(landlordId: String) {
        val db = FirebaseFirestore.getInstance()
        try {
            val query = db.collection("listings")
                .whereEqualTo("landlordId", landlordId)
                .whereEqualTo("isSample", true)
                .get()
                .await()

            query.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue even if deletion fails - we'll just overwrite
        }
    }

    private suspend fun seedSampleReviewsForListing(
        listingId: String,
        recommendedCount: Int,
        notRecommendedCount: Int
    ) {
        if (listingId.isBlank()) return
        val total = recommendedCount + notRecommendedCount
        if (total <= 0) return
        val db = FirebaseFirestore.getInstance()
        val reviewsRef = db.collection("listings").document(listingId).collection("reviews")
        val now = System.currentTimeMillis()

        for (i in 0 until recommendedCount) {
            val comment = positiveComments[i % positiveComments.size]
            val review = mapOf(
                "listingId" to listingId,
                "reviewerId" to "seed_tenant_${(i % 5) + 1}",
                "recommended" to true,
                "comment" to comment,
                "createdAt" to Timestamp(Date(now - (i + 1L) * 86_400_000L))
            )
            reviewsRef.add(review).await()
        }

        for (i in 0 until notRecommendedCount) {
            val comment = negativeComments[i % negativeComments.size]
            val review = mapOf(
                "listingId" to listingId,
                "reviewerId" to "seed_tenant_${(i % 5) + 1}",
                "recommended" to false,
                "comment" to comment,
                "createdAt" to Timestamp(Date(now - (i + 1L) * 86_400_000L))
            )
            reviewsRef.add(review).await()
        }

        val summary = mapOf(
            "total" to total,
            "recommendedCount" to recommendedCount,
            "notRecommendedCount" to notRecommendedCount,
            "updatedAt" to Timestamp.now()
        )
        db.collection("listings").document(listingId).update("reviewSummary", summary).await()
    }
}

