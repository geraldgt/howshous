package io.github.howshous.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yalantis.ucrop.UCrop
import java.io.File

fun buildCropIntent(
    context: Context,
    sourceUri: Uri,
    aspectRatio: Pair<Float, Float>? = null,
    freeStyle: Boolean = true
): Intent {
    val destinationUri = Uri.fromFile(
        File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
    )
    val uCrop = UCrop.of(sourceUri, destinationUri)
    if (aspectRatio != null) {
        uCrop.withAspectRatio(aspectRatio.first, aspectRatio.second)
    }
    val options = UCrop.Options().apply {
        setFreeStyleCropEnabled(freeStyle)
    }
    return uCrop.withOptions(options)
        .getIntent(context)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

fun getCroppedUri(data: Intent?): Uri? = data?.let { UCrop.getOutput(it) }

fun getCropError(data: Intent?): Throwable? = data?.let { UCrop.getError(it) }
