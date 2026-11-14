package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaStorageManager(private val context: Context) {
    private val mediaDir: File by lazy {
        File(context.filesDir, "spleex_media").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun persistImageFromUri(sourceUri: Uri, maxDimension: Int = 1280): String =
        withContext(Dispatchers.IO) {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val sampleSize = calculateInSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                maxDimension,
                maxDimension
            )

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            }

            val outputFile = File(mediaDir, "spleex_${UUID.randomUUID()}.jpg")
            if (bitmap != null) {
                val scaled = scaleBitmapIfNeeded(bitmap, maxDimension)
                FileOutputStream(outputFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
                }
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(outputFile).use { out ->
                        input.copyTo(out)
                    }
                }
            }
            Uri.fromFile(outputFile).toString()
        }

    fun drawableUri(drawableResId: Int): String {
        return "android.resource://${context.packageName}/$drawableResId"
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (width >= height) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
