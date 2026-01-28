package io.github.howshous.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

suspend fun uploadCompressedImage(
    context: Context,
    uri: Uri,
    path: String
): String {

    val storageRef = FirebaseStorage.getInstance().reference.child(path)

    // Read file stream
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val bitmap = inputStream?.use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: throw IllegalStateException("Unable to decode image.")

    // Compress JPEG
    val jpg = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, jpg)
    val compressedBytes = jpg.toByteArray()

    // Upload
    storageRef.putBytes(compressedBytes).await()

    // Get URL
    return storageRef.downloadUrl.await().toString()
}
