package com.eltavine.duckparse.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.atan

fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this

    val radians = Math.toRadians(degrees.toDouble())
    val sin = abs(kotlin.math.sin(radians)).toFloat()
    val cos = abs(kotlin.math.cos(radians)).toFloat()

    val newWidth = (height * sin + width * cos).toInt()
    val newHeight = (height * cos + width * sin).toInt()

    val rotated = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.RGB_565)
    val canvas = Canvas(rotated)
    val matrix = Matrix().apply {
        postTranslate(-width / 2f, -height / 2f)
        postRotate(degrees)
        postTranslate(newWidth / 2f, newHeight / 2f)
    }
    canvas.drawBitmap(this, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return rotated
}

fun Bitmap.deskew(skewAngleDegrees: Float): Bitmap {
    if (skewAngleDegrees == 0f) return this

    val tanA = kotlin.math.tan(Math.toRadians(skewAngleDegrees.toDouble())).toFloat()
    val extraWidth = (abs(tanA) * height).toInt()
    val newWidth = width + extraWidth

    val deskewed = Bitmap.createBitmap(newWidth, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(deskewed)
    val matrix = Matrix().apply {
        setSkew(tanA, 0f)
        postTranslate(if (tanA < 0) extraWidth.toFloat() else 0f, 0f)
    }
    canvas.drawBitmap(this, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return deskewed
}

fun Bitmap.grayscale(): IntArray {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return IntArray(width * height) { i ->
        val px = pixels[i]
        val r = (px shr 16) and 0xFF
        val g = (px shr 8) and 0xFF
        val b = px and 0xFF
        (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
    }
}

fun IntArray.toGrayscaleBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val pixels = IntArray(width * height) { i ->
        val gray = this[i].coerceIn(0, 255)
        (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

fun Bitmap.clone(): Bitmap = copy(config, isMutable)
