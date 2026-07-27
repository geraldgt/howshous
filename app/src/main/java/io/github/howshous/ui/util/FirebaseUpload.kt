package io.github.howshous.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.howshous.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.UUID

suspend fun uploadCompressedImage(
    context: Context,
    uri: Uri,
    path: String
): String {
    return withContext(Dispatchers.IO) {
        val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
        if (cloudName.isBlank() || preset.isBlank()) {
            throw IllegalStateException(
                "Cloudinary is not configured. Add CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET to local.properties."
            )
        }

        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = inputStream?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Unable to decode image.")

        val jpg = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, jpg)
        val compressedBytes = jpg.toByteArray()

        val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
        val folder = path.substringBeforeLast("/", "").trim()
        val publicIdBase = path.substringAfterLast("/", "img")
            .substringBeforeLast(".")
            .ifBlank { "img" }
        val publicId = "${publicIdBase}_${UUID.randomUUID()}"

        val formBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart(
                "file",
                "$publicId.jpg",
                compressedBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .addFormDataPart("public_id", publicId)
            .addFormDataPart("resource_type", "image")
        if (folder.isNotBlank()) {
            formBuilder.addFormDataPart("folder", folder)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(formBuilder.build())
            .build()

        val client = OkHttpClient()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Cloudinary upload failed (${response.code}): $body")
            }
            val json = JSONObject(body)
            val secureUrl = json.optString("secure_url", "")
            if (secureUrl.isBlank()) {
                throw IOException("Cloudinary upload did not return secure_url.")
            }
            secureUrl
        }
    }
}
