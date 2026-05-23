package com.eltavine.duckparse.parser

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object QrDecoder {

    private const val DD_PREFIX = "DD|"

    suspend fun decode(bitmap: Bitmap): List<String> = withContext(Dispatchers.Default) {
        val scanner = BarcodeScanning.getClient()
        val results = mutableListOf<Barcode>()

        // Try original image
        results.addAll(processBitmap(scanner, bitmap))

        // If no DD code found, try with enhanced contrast (helps with coloured QR codes)
        if (results.none { it.displayValue?.startsWith(DD_PREFIX) == true }) {
            val enhanced = enhanceContrast(bitmap)
            if (enhanced != null) {
                results.addAll(processBitmap(scanner, enhanced))
            }
        }

        results
            .filter { it.valueType == Barcode.TYPE_TEXT }
            .mapNotNull { it.displayValue }
            .filter { it.startsWith(DD_PREFIX) }
    }

    private suspend fun processBitmap(
        scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        bitmap: Bitmap,
    ): List<Barcode> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            scanner.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
    }

    /**
     * Boost contrast for gradient-coloured QR codes that ML Kit
     * might miss on the first pass.
     */
    private fun enhanceContrast(source: Bitmap): Bitmap? {
        return try {
            val pixels = IntArray(source.width * source.height)
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

            // Find min/max luminance
            var minLum = 255
            var maxLum = 0
            for (p in pixels) {
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum < minLum) minLum = lum
                if (lum > maxLum) maxLum = lum
            }

            if (maxLum <= minLum) return null

            // Stretch contrast so darkest → 0, lightest → 255
            val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
            val outPixels = IntArray(pixels.size)
            for (i in pixels.indices) {
                val r = (pixels[i] shr 16) and 0xFF
                val g = (pixels[i] shr 8) and 0xFF
                val b = pixels[i] and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                val stretched = ((lum - minLum) * 255 / (maxLum - minLum)).coerceIn(0, 255)
                outPixels[i] = (0xFF shl 24) or (stretched shl 16) or (stretched shl 8) or stretched
            }
            out.setPixels(outPixels, 0, source.width, 0, 0, source.width, source.height)
            out
        } catch (_: Exception) {
            null
        }
    }
}
