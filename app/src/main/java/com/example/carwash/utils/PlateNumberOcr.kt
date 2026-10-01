package com.example.carwash.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PlateNumberOcr {

    suspend fun recognizePlateNumber(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val visionText = recognizer.process(inputImage).await()
            val text = visionText.text

            Log.d("PlateNumberOcr", "OCR Raw Text: $text")

            val candidate = findBestPlateCandidate(text)
            candidate
        } catch (e: Exception) {
            Log.e("PlateNumberOcr", "Error recognizing plate number: ${e.message}")
            null
        }
    }

    private fun findBestPlateCandidate(fullText: String): String? {
        if (fullText.isBlank()) return null

        val lines = fullText.split("\n", "\r").map { it.trim().uppercase() }

        for (line in lines) {
            val cleaned = line.replace(Regex("[^A-Z0-9\\-\\s]"), "").trim()
            val hasLetters = cleaned.any { it.isLetter() }
            val hasDigits = cleaned.any { it.isDigit() }

            if (cleaned.length in 5..10 && hasLetters && hasDigits) {
                return formatPlateNumber(cleaned)
            }
        }

        return null
    }

    private fun formatPlateNumber(raw: String): String {
        val parts = raw.split(" ", "-").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            return "${parts[0]}-${parts[1]}"
        }
        return raw.replace(" ", "-")
    }
}
