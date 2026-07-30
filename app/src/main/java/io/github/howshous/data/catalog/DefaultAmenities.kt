package io.github.howshous.data.catalog

object DefaultAmenities {
    val labels = listOf(
        "Free Parking",
        "WiFi",
        "Air Conditioning",
        "Pets Allowed",
        "Kitchen Access",
        "Laundry",
        "Security",
        "CCTV",
        "Furnished",
        "Near Public Transport",
        "Gym Access",
        "Swimming Pool",
    )

    fun normalizeLabel(label: String): String =
        label.trim().replace(Regex("\\s+"), " ")

    fun documentIdFor(label: String): String =
        normalizeLabel(label)
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "amenity" }

}
