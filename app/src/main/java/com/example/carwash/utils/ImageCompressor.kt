package com.example.carwash.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val MAX_BYTES = 250 * 1024
    private const val MAX_DIMENSION = 1200

    suspend fun compress(context: Context, sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
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

            try {
                val exiOut = ExifInterface(output.absolutePath)
                exiOut.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                exiOut.saveAttributes()
            } catch (e: Exception) {
                Log.e("ImageCompressor", "Error setting EXIF normal: ${e.message}")
            }

            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            Uri.fromFile(output)
        } catch (e: Exception) {
            Log.e("ImageCompressor", "Failed to compress image, returning sourceUri: ${e.message}")
            sourceUri
        }
    }

    private fun getRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val exifInterface = ExifInterface(pfd.fileDescriptor)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                pfd.close()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } else 0
        } catch (e: Exception) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exifInterface = ExifInterface(stream)
                    val orientation = exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
            } catch (_: Exception) {
                0
            }
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

        var resultBitmap = decodedBitmap

        if (rotationDegrees != 0) {
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
            resultBitmap = rotated
        }

        return resultBitmap
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
