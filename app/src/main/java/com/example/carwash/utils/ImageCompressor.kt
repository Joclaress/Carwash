package com.example.carwash.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val MAX_BYTES = 250 * 1024
    private const val MAX_DIMENSION = 1200

    suspend fun compress(context: Context, sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(context, sourceUri)
        val bytes = compressBitmap(bitmap)
        val folder = File(context.cacheDir, "compressed")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val output = File.createTempFile("compressed_", ".jpg", folder)

        FileOutputStream(output).use {
            it.write(bytes)
        }
        
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )
    }

    private fun getRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun decodeBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {
        val rotationDegrees = getRotationDegrees(context, uri)

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)
        options.inJustDecodeBounds = false

        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw Exception("Unable to decode image")

        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            val rotated = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
            if (rotated != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotated
        } else {
            decodedBitmap
        }
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int
    ): Int {
        var sample = 1
        while (width / sample > MAX_DIMENSION || height / sample > MAX_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        var quality = 80
        val output = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        
        while (output.size() > MAX_BYTES && quality > 30) {
            quality -= 10
            output.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
        
        return output.toByteArray()
    }
}
