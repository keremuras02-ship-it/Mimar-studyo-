package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtils {

    /**
     * Safely decodes a picked image URI and scales it down to maxDimension px
     * so inline multimodal payloads stay well below Gemini's 20MB limit.
     */
    suspend fun decodeScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return@withContext null

            var sampleSize = 1
            val largest = max(srcW, srcH)
            while (largest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            scaleBitmapIfNeeded(decoded, maxDimension)
        } catch (_: Exception) {
            null
        }
    }

    fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / largest.toFloat()
        val newW = (w * ratio).roundToInt().coerceAtLeast(1)
        val newH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * Compresses a render bitmap to a lightweight Base64 JPEG thumbnail
     * guaranteed to fit comfortably within Firestore's 350KB thumbnail rule bound.
     */
    suspend fun bitmapToFirestoreThumbnail(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val scaled = scaleBitmapIfNeeded(bitmap, 640)
        var quality = 76
        var encoded: String
        do {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            quality -= 12
        } while (encoded.length > 320_000 && quality >= 28)
        encoded
    }

    fun base64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun shareTextReport(context: Context, subject: String, body: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(shareIntent, subject))
    }
}
