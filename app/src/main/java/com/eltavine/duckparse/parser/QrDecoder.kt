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
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        val results = suspendCancellableCoroutine<List<Barcode>> { cont ->
            scanner.process(image)
                .addOnSuccessListener { barcodes -> cont.resume(barcodes) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

        results
            .filter { it.valueType == Barcode.TYPE_TEXT }
            .mapNotNull { it.displayValue }
            .filter { it.startsWith(DD_PREFIX) }
    }
}
