package com.example.colorsensor.utils

import android.graphics.Bitmap
import android.graphics.Color

fun getBrightness(color: Int): Int {
    return ((Color.red(color) * 0.299) + (Color.green(color) * 0.587) + (Color.blue(color) * 0.114)).toInt()
}

fun adjustColorBrightness(baseColor: Int, targetBrightness: Int): Int {
    val r = Color.red(baseColor)
    val g = Color.green(baseColor)
    val b = Color.blue(baseColor)

    val currentBrightness = getBrightness(baseColor)
    if (currentBrightness == 0) return baseColor

    val ratio = targetBrightness.toFloat() / currentBrightness

    val newR = (r * ratio).coerceIn(0f, 255f).toInt()
    val newG = (g * ratio).coerceIn(0f, 255f).toInt()
    val newB = (b * ratio).coerceIn(0f, 255f).toInt()

    return Color.rgb(newR, newG, newB)
}

fun isEdgePixel(pixel: Int): Boolean {
    return pixel == Color.WHITE
}

fun floodFillWallRegion(
    cannyBitmap: Bitmap,
    originalBitmap: Bitmap,
    startX: Int,
    startY: Int
): List<Pair<Int, Int>> {
    val width = originalBitmap.width
    val height = originalBitmap.height
    val visited = Array(height) { BooleanArray(width) }
    val region = mutableListOf<Pair<Int, Int>>()
    val queue = ArrayDeque<Pair<Int, Int>>()

    queue.add(Pair(startX, startY))
    visited[startY][startX] = true

    val directions = arrayOf(
        Pair(0, 1), Pair(1, 0), Pair(0, -1), Pair(-1, 0),
        Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
    )

    while (queue.isNotEmpty()) {
        val (x, y) = queue.removeFirst()
        region.add(Pair(x, y))

        for ((dx, dy) in directions) {
            val newX = x + dx
            val newY = y + dy

            if (
                newX in 0 until width &&
                newY in 0 until height &&
                !visited[newY][newX] &&
                !isEdgePixel(cannyBitmap.getPixel(newX, newY))
            ) {
                visited[newY][newX] = true
                queue.add(Pair(newX, newY))
            }
        }
    }

    return region
}
fun edgeAwareColorReplace(
    originalBitmap: Bitmap,
    cannyBitmap: Bitmap,
    startX: Int,
    startY: Int,
    targetColor: Int
): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height
    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

    val regionPixels = floodFillWallRegion(cannyBitmap, originalBitmap, startX, startY)
    val averageBrightness = regionPixels.map { (x, y) ->
        getBrightness(originalBitmap.getPixel(x, y))
    }.average().toInt()

    val adjustedColor = adjustColorBrightness(targetColor, averageBrightness)

    for ((x, y) in regionPixels) {
        resultBitmap.setPixel(x, y, adjustedColor)
    }

    return resultBitmap
}
