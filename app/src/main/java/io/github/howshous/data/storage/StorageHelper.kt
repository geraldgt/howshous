package io.github.howshous.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageHelper {
    private val storage = FirebaseStorage.getInstance()

    // returns download URL as string
    suspend fun uploadUserImage(uid: String, uri: Uri, pathSuffix: String = "profile.jpg"): String {
        val ref = storage.reference.child("user_uploads/$uid/${UUID.randomUUID()}_$pathSuffix")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadListingImage(listingId: String, uri: Uri): String {
        val ref = storage.reference.child("listing_uploads/$listingId/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
