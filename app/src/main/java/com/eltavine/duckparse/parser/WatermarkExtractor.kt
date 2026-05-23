package com.eltavine.duckparse.parser

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object WatermarkExtractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val identityKeywords = setOf(
        "Xiaomi", "Samsung", "Google", "OnePlus", "OPPO", "vivo",
        "Huawei", "Honor", "realme", "Motorola", "Nokia", "Sony",
        "Qualcomm", "MediaTek", "SM", "SDM", "MT", "Exynos",
        "Android", "SDK",
    )

    private val modelPattern = Regex("[A-Z]{2,4}\\d")
    private val sdkPattern = Regex("SDK\\s*\\d+")
    private val androidPattern = Regex("Android\\s*\\d+")

    suspend fun extract(bitmap: Bitmap): List<String> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val text = suspendCancellableCoroutine<String> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { cont.resume("") }
        }

        text.lines()
            .map { it.trim() }
            .filter { line -> line.length >= 6 && looksLikeIdentity(line) }
    }

    private fun looksLikeIdentity(text: String): Boolean {
        if (identityKeywords.any { text.contains(it, ignoreCase = true) }) return true
        if (modelPattern.containsMatchIn(text)) return true
        if (sdkPattern.containsMatchIn(text)) return true
        if (androidPattern.containsMatchIn(text)) return true
        return false
    }
}
