package com.eltavine.duckparse.parser

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

enum class WatermarkPreprocessing { NONE, AUTO, BLIND, SCREENSHOT }

object WatermarkExtractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val identityKeywords = setOf(
        "Xiaomi", "Samsung", "Google", "OnePlus", "OPPO", "vivo",
        "Huawei", "Honor", "realme", "Motorola", "Nokia", "Sony",
        "Qualcomm", "MediaTek", "SM", "SDM", "MT", "Exynos",
        "Android", "SDK",
    )

    private val modelPattern = Regex("[A-Z]{2,4}\\d+")
    private val sdkPattern = Regex("SDK\\s*\\d+")
    private val androidPattern = Regex("Android\\s*\\d+")

    suspend fun extract(
        bitmap: Bitmap,
        preprocessing: WatermarkPreprocessing = WatermarkPreprocessing.AUTO,
    ): List<String> = withContext(Dispatchers.Default) {
        when (preprocessing) {
            WatermarkPreprocessing.NONE -> ocrAndFilter(bitmap)
            WatermarkPreprocessing.AUTO -> extractAuto(bitmap)
            WatermarkPreprocessing.BLIND -> {
                val processed = ImagePreprocessor.preprocessForBlindWatermark(bitmap)
                val result = ocrAndFilter(processed)
                processed.recycle()
                result
            }
            WatermarkPreprocessing.SCREENSHOT -> {
                val processed = ImagePreprocessor.preprocessForScreenshotWatermark(bitmap)
                val result = ocrAndFilter(processed)
                processed.recycle()
                result
            }
        }
    }

    private suspend fun extractAuto(bitmap: Bitmap): List<String> {
        val originalDeferred = async { ocrAndFilter(bitmap) }
        val blindDeferred = async {
            val processed = ImagePreprocessor.preprocessForBlindWatermark(bitmap)
            val result = ocrAndFilter(processed)
            processed.recycle()
            result
        }
        val screenshotDeferred = async {
            val processed = ImagePreprocessor.preprocessForScreenshotWatermark(bitmap)
            val result = ocrAndFilter(processed)
            processed.recycle()
            result
        }

        val (original, blind, screenshot) = awaitAll(originalDeferred, blindDeferred, screenshotDeferred)

        val merged = mutableSetOf<String>()
        merged.addAll(original)
        merged.addAll(blind)
        merged.addAll(screenshot)

        return deduplicate(merged.toList())
    }

    private fun deduplicate(lines: List<String>): List<String> {
        if (lines.isEmpty()) return emptyList()
        if (lines.size == 1) return lines

        val groups = mutableListOf<MutableList<String>>()
        val remaining = lines.toMutableList()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val group = mutableListOf(seed)
            val iter = remaining.iterator()
            while (iter.hasNext()) {
                val candidate = iter.next()
                if (levenshteinRatio(seed, candidate) > 0.7) {
                    group.add(candidate)
                    iter.remove()
                }
            }
            groups.add(group)
        }

        return groups.map { group ->
            group.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: group.first()
        }
    }

    private fun levenshteinRatio(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(a, b)
        return 1.0 - dist.toDouble() / maxLen
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost,
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[n]
    }

    private suspend fun ocrAndFilter(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)

        val text = suspendCancellableCoroutine<String> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { cont.resume("") }
        }

        return text.lines()
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
