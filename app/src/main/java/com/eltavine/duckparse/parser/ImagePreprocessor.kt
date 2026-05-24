package com.eltavine.duckparse.parser

import android.graphics.Bitmap
import com.eltavine.duckparse.util.deskew
import com.eltavine.duckparse.util.grayscale
import com.eltavine.duckparse.util.rotate
import com.eltavine.duckparse.util.toGrayscaleBitmap

object ImagePreprocessor {

    private const val TILE_SIZE = 8
    private const val CLIP_LIMIT = 2.0f
    private const val THRESHOLD_WINDOW = 15
    private const val THRESHOLD_CONSTANT = 7
    private const val BLIND_SKEW_DEGREES = 20f
    private const val SCREENSHOT_ROTATE_DEGREES = 30f

    fun preprocessForBlindWatermark(source: Bitmap): Bitmap {
        val gray = source.grayscale()
        val clahe = applyClahe(gray, source.width, source.height)
        var bitmap = clahe.toGrayscaleBitmap(source.width, source.height)

        bitmap = bitmap.deskew(BLIND_SKEW_DEGREES)

        val deskewedGray = bitmap.grayscale()
        val thresholded = adaptiveThreshold(deskewedGray, bitmap.width, bitmap.height)
        val dilated = morphologicalDilate(thresholded, bitmap.width, bitmap.height)

        return dilated.toGrayscaleBitmap(bitmap.width, bitmap.height)
    }

    fun preprocessForScreenshotWatermark(source: Bitmap): Bitmap {
        val gray = source.grayscale()
        val clahe = applyClahe(gray, source.width, source.height)
        var bitmap = clahe.toGrayscaleBitmap(source.width, source.height)

        bitmap = bitmap.rotate(SCREENSHOT_ROTATE_DEGREES)

        val rotatedGray = bitmap.grayscale()
        val thresholded = adaptiveThreshold(rotatedGray, bitmap.width, bitmap.height)

        return thresholded.toGrayscaleBitmap(bitmap.width, bitmap.height)
    }

    fun applyClahe(gray: IntArray, w: Int, h: Int): IntArray {
        val result = IntArray(w * h)
        val tileW = TILE_SIZE
        val tileH = TILE_SIZE
        val tilesX = (w + tileW - 1) / tileW
        val tilesY = (h + tileH - 1) / tileH

        val tiles = Array(tilesY) { Array(tilesX) { IntArray(256) } }
        val luts = Array(tilesY) { Array(tilesX) { IntArray(256) } }

        val pixelsPerTile = tileW * tileH
        val clipLimit = (CLIP_LIMIT * pixelsPerTile / 256).toInt().coerceAtLeast(1)

        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                val hist = tiles[ty][tx]
                val xStart = tx * tileW
                val yStart = ty * tileH
                for (dy in 0 until tileH) {
                    val y = yStart + dy
                    if (y >= h) break
                    for (dx in 0 until tileW) {
                        val x = xStart + dx
                        if (x >= w) break
                        hist[gray[y * w + x]]++
                    }
                }

                var clipped = 0
                for (i in 0 until 256) {
                    if (hist[i] > clipLimit) {
                        clipped += hist[i] - clipLimit
                        hist[i] = clipLimit
                    }
                }
                val redist = clipped / 256
                for (i in 0 until 256) {
                    hist[i] = (hist[i] + redist).coerceAtMost(clipLimit)
                }

                val lut = luts[ty][tx]
                var sum = 0
                for (i in 0 until 256) {
                    sum += hist[i]
                    lut[i] = (sum * 255 / pixelsPerTile).coerceIn(0, 255)
                }
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val tx = (x / tileW).coerceIn(0, tilesX - 1)
                val ty = (y / tileH).coerceIn(0, tilesY - 1)

                val leftX = tx * tileW
                val topY = ty * tileH
                val tx2 = (tx + 1).coerceAtMost(tilesX - 1)
                val ty2 = (ty + 1).coerceAtMost(tilesY - 1)

                val fx = if (tileW > 1) (x - leftX).toFloat() / tileW else 0f
                val fy = if (tileH > 1) (y - topY).toFloat() / tileH else 0f

                val lut00 = luts[ty][tx]
                val lut10 = luts[ty][tx2]
                val lut01 = luts[ty2][tx]
                val lut11 = luts[ty2][tx2]

                val val0 = lut00[gray[y * w + x]]
                val val1 = lut10[gray[y * w + x]]
                val val2 = lut01[gray[y * w + x]]
                val val3 = lut11[gray[y * w + x]]

                val top = val0 + (val1 - val0) * fx
                val bottom = val2 + (val3 - val2) * fx
                result[y * w + x] = (top + (bottom - top) * fy).toInt().coerceIn(0, 255)
            }
        }

        return result
    }

    fun adaptiveThreshold(gray: IntArray, w: Int, h: Int): IntArray {
        val result = IntArray(w * h)
        val half = THRESHOLD_WINDOW / 2
        val integral = buildIntegralImage(gray, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val x1 = (x - half).coerceAtLeast(0)
                val y1 = (y - half).coerceAtLeast(0)
                val x2 = (x + half).coerceAtMost(w - 1)
                val y2 = (y + half).coerceAtMost(h - 1)

                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                val sum = integralSum(integral, x1, y1, x2, y2, w)
                val mean = sum / count

                result[y * w + x] = if (gray[y * w + x] > mean - THRESHOLD_CONSTANT) 255 else 0
            }
        }
        return result
    }

    fun morphologicalDilate(binary: IntArray, w: Int, h: Int): IntArray {
        val result = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var maxVal = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val ny = (y + dy).coerceIn(0, h - 1)
                        if (binary[ny * w + nx] > maxVal) maxVal = binary[ny * w + nx]
                    }
                }
                result[y * w + x] = maxVal
            }
        }
        return result
    }

    private fun buildIntegralImage(gray: IntArray, w: Int, h: Int): LongArray {
        val stride = w + 1
        val integral = LongArray(stride * (h + 1))
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = (y + 1) * stride + (x + 1)
                integral[idx] = gray[y * w + x].toLong() +
                    integral[y * stride + (x + 1)] +
                    integral[(y + 1) * stride + x] -
                    integral[y * stride + x]
            }
        }
        return integral
    }

    private fun integralSum(integral: LongArray, x1: Int, y1: Int, x2: Int, y2: Int, w: Int): Long {
        val stride = w + 1
        return integral[(y2 + 1) * stride + (x2 + 1)] -
            integral[y1 * stride + (x2 + 1)] -
            integral[(y2 + 1) * stride + x1] +
            integral[y1 * stride + x1]
    }
}
