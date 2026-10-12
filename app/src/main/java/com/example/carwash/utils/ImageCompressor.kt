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
import kotlin.math.max

object ImageCompressor {

    private const val MAX_BYTES = 300 * 1024
    private const val MAX_DIMENSION = 1200

    suspend fun compress(context: Context, sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBitmap(context, sourceUri)
                ?: return@withContext sourceUri

            val bytes = compressBitmap(bitmap)

            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            if (bytes.isEmpty()) {
                return@withContext sourceUri
            }

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
            } catch (e: Throwable) {
                Log.e("ImageCompressor", "Error setting EXIF normal: ${e.message}")
            }

            Uri.fromFile(output)
        } catch (t: Throwable) {
            Log.e("ImageCompressor", "Failed to compress image, returning sourceUri: ${t.message}", t)
            sourceUri
        }
    }

    private fun getRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            val filePath = if (uri.scheme == "file") uri.path else null
            val exifInterface = if (filePath != null) {
                ExifInterface(filePath)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    ExifInterface(pfd.fileDescriptor)
                }
            }
            if (exifInterface != null) {
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
            } else 0
        } catch (t: Throwable) {
            Log.e("ImageCompressor", "getRotationDegrees error: ${t.message}")
            0
        }
    }

    private fun decodeBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? {
        return try {
            val rotationDegrees = getRotationDegrees(context, uri)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (uri.scheme == "file" && uri.path != null) {
                BitmapFactory.decodeFile(uri.path, options)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }

            val outW = max(options.outWidth, 1)
            val outH = max(options.outHeight, 1)

            options.inSampleSize = calculateSampleSize(outW, outH)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val decodedBitmap = if (uri.scheme == "file" && uri.path != null) {
                BitmapFactory.decodeFile(uri.path, options)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } ?: return null

            var resultBitmap = decodedBitmap

            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                try {
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
                } catch (t: Throwable) {
                    Log.e("ImageCompressor", "Rotation failed due to memory, using unrotated bitmap: ${t.message}")
                }
            }

            resultBitmap
        } catch (t: Throwable) {
            Log.e("ImageCompressor", "decodeBitmap error: ${t.message}", t)
            null
        }
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int
    ): Int {
        var sample = 1
        val w = max(width, 1)
        val h = max(height, 1)
        while (w / sample > MAX_DIMENSION || h / sample > MAX_DIMENSION) {
            sample *= 2
        }
        return max(sample, 1)
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        var quality = 80
        val output = ByteArrayOutputStream()

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)

            while (output.size() > MAX_BYTES && quality > 30) {
                quality -= 10
                output.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
        } catch (t: Throwable) {
            Log.e("ImageCompressor", "compressBitmap error: ${t.message}")
        }

        return output.toByteArray()
    }
}
