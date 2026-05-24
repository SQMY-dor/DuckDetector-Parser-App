package com.eltavine.duckparse.parser

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

class CameraFrameAnalyzer(
    private val onQrDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var frameCounter = 0
    private var isAnalyzing = false

    private val yuvBytes = ByteArray(640 * 480 * 3 / 2)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isAnalyzing) {
            imageProxy.close()
            return
        }

        frameCounter++
        if (frameCounter % 6 != 0) {
            imageProxy.close()
            return
        }

        if (imageProxy.format != ImageFormat.YUV_420_888) {
            imageProxy.close()
            return
        }

        val bitmap = yuvToRgb(imageProxy) ?: run {
            imageProxy.close()
            return
        }

        isAnalyzing = true
        val inputImage = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnCompleteListener {
                isAnalyzing = false
                imageProxy.close()
                bitmap.recycle()

                val barcodes = it.result
                val qrText = barcodes
                    ?.filter { barcode ->
                        barcode.valueType == Barcode.TYPE_TEXT &&
                            barcode.displayValue?.startsWith(DD_PREFIX) == true
                    }
                    ?.mapNotNull { it.displayValue }
                    ?.firstOrNull()

                if (qrText != null) {
                    onQrDetected(qrText)
                }
            }
            .addOnFailureListener {
                isAnalyzing = false
                imageProxy.close()
                bitmap.recycle()
            }
    }

    companion object {
        private const val DD_PREFIX = "DD|"
    }
}

private fun yuvToRgb(image: ImageProxy): Bitmap? {
    val planes = image.planes
    if (planes.size < 3) return null

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer: ByteBuffer = yPlane.buffer
    val uBuffer: ByteBuffer = uPlane.buffer
    val vBuffer: ByteBuffer = vPlane.buffer

    val width = image.width
    val height = image.height

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val pixels = IntArray(width * height)

    val yRowStride = yPlane.rowStride
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride

    for (y in 0 until height) {
        for (x in 0 until width) {
            val yVal = (yBuffer.get(y * yRowStride + x).toInt() and 0xFF)
            val uvIdx = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
            val uVal = (uBuffer.get(uvIdx).toInt() and 0xFF) - 128
            val vVal = (vBuffer.get(uvIdx).toInt() and 0xFF) - 128

            var r = yVal + (1.402 * vVal).toInt()
            var g = yVal - (0.344 * uVal).toInt() - (0.714 * vVal).toInt()
            var b = yVal + (1.772 * uVal).toInt()

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
