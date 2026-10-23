package com.m.adacolorytrazo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {
    const val MAX_BITMAP_SIDE = 1536

    /**
     * Decodifica un recurso sin que la densidad del dispositivo lo amplíe y
     * mantiene un tamaño razonable para el editor y su historial.
     */
    fun decodeResourceMutable(
        context: Context,
        resId: Int,
        maxSide: Int = MAX_BITMAP_SIDE
    ): Bitmap {
        require(resId != 0) { "Drawable not found: $resId" }
        val resources = context.applicationContext.resources
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(resources, resId, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "No se pudo leer el tamaño del dibujo"
        }

        val safeSide = maxSide.coerceAtLeast(1)
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > safeSide) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
            inScaled = false
        }
        val decoded = BitmapFactory.decodeResource(resources, resId, options)
            ?: error("No se pudo decodificar el dibujo")
        return if (decoded.config == Bitmap.Config.ARGB_8888 && decoded.isMutable) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    fun drawableToMutableBitmap(
        ctx: Context,
        resId: Int,
        maxSide: Int = MAX_BITMAP_SIDE
    ): Bitmap {
        val d: Drawable = ContextCompat.getDrawable(ctx.applicationContext, resId)
            ?: error("Drawable not found: $resId")
        val bmp = when (d) {
            is BitmapDrawable -> d.bitmap.copy(Bitmap.Config.ARGB_8888, true)
            else -> {
                val intrinsicW = if (d.intrinsicWidth > 0) d.intrinsicWidth else 1024
                val intrinsicH = if (d.intrinsicHeight > 0) d.intrinsicHeight else 1024
                val scale = min(
                    1f,
                    maxSide.coerceAtLeast(1).toFloat() / max(intrinsicW, intrinsicH)
                )
                val w = (intrinsicW * scale).toInt().coerceAtLeast(1)
                val h = (intrinsicH * scale).toInt().coerceAtLeast(1)
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(b).also {
                    d.setBounds(0, 0, w, h)
                    d.draw(it)
                }
                b
            }
        }
        return scaleDownIfNeeded(bmp, maxSide)
    }

    /** Devuelve una copia coloreada o null si la región no cambió. */
    fun floodFill(
        source: Bitmap,
        x: Int,
        y: Int,
        newColor: Int,
        tolerance: Int = 12,
        blackEdgeThreshold: Int = 60
    ): Bitmap? {
        val w = source.width
        val h = source.height
        if (x !in 0 until w || y !in 0 until h) return null

        val target = source.getPixel(x, y)
        if (colorsClose(target, newColor, 0) || isDark(target, blackEdgeThreshold)) return null
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixelCount = w.toLong() * h.toLong()
        require(pixelCount <= Int.MAX_VALUE) { "Bitmap demasiado grande para rellenar" }
        val visited = BooleanArray(pixelCount.toInt())
        val queue = IntArray(pixelCount.toInt())
        var head = 0
        var tail = 0
        var changed = false

        fun enqueue(px: Int, py: Int) {
            if (px !in 0 until w || py !in 0 until h) return
            val index = py * w + px
            if (!visited[index]) {
                visited[index] = true
                queue[tail++] = index
            }
        }

        enqueue(x, y)
        while (head < tail) {
            val index = queue[head++]
            val px = index % w
            val py = index / w
            val color = result.getPixel(px, py)
            if (isDark(color, blackEdgeThreshold) || !colorsClose(color, target, tolerance)) {
                continue
            }
            result.setPixel(px, py, newColor)
            changed = true
            enqueue(px + 1, py)
            enqueue(px - 1, py)
            enqueue(px, py + 1)
            enqueue(px, py - 1)
        }
        return result.takeIf { changed }
    }

    /** Pinta un trazo sobre las zonas claras del dibujo, conservando el contorno oscuro. */
    fun paintStroke(
        source: Bitmap,
        points: List<PointF>,
        newColor: Int,
        strokeWidth: Float,
        blackEdgeThreshold: Int = 105
    ): Bitmap? {
        if (points.isEmpty() || strokeWidth <= 0f) return null
        val w = source.width
        val h = source.height
        val padding = strokeWidth / 2f + 2f
        val left = floor(points.minOf { it.x } - padding).toInt().coerceAtLeast(0)
        val top = floor(points.minOf { it.y } - padding).toInt().coerceAtLeast(0)
        val right = ceil(points.maxOf { it.x } + padding).toInt().coerceAtMost(w - 1)
        val bottom = ceil(points.maxOf { it.y } + padding).toInt().coerceAtMost(h - 1)
        if (left > right || top > bottom) return null

        val regionWidth = right - left + 1
        val regionHeight = bottom - top + 1
        val mask = Bitmap.createBitmap(regionWidth, regionHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth
        }
        if (points.size == 1) {
            canvas.drawCircle(
                points[0].x - left,
                points[0].y - top,
                strokeWidth / 2f,
                paint
            )
        } else {
            val path = Path().apply {
                moveTo(points.first().x - left, points.first().y - top)
                points.drop(1).forEach { lineTo(it.x - left, it.y - top) }
            }
            canvas.drawPath(path, paint)
        }

        val maskPixels = IntArray(regionWidth * regionHeight)
        val sourcePixels = IntArray(regionWidth * regionHeight)
        mask.getPixels(maskPixels, 0, regionWidth, 0, 0, regionWidth, regionHeight)
        source.getPixels(sourcePixels, 0, regionWidth, left, top, regionWidth, regionHeight)
        val resultPixels = sourcePixels.copyOf()
        val colorAlpha = Color.alpha(newColor) / 255f
        var changed = false

        for (index in sourcePixels.indices) {
            val coverage = Color.alpha(maskPixels[index]) / 255f * colorAlpha
            if (coverage <= 0f) continue
            val original = sourcePixels[index]
            if (Color.alpha(original) < 16 || isDark(original, blackEdgeThreshold)) continue

            val inverse = 1f - coverage
            val red = (Color.red(original) * inverse + Color.red(newColor) * coverage).toInt()
            val green = (Color.green(original) * inverse + Color.green(newColor) * coverage).toInt()
            val blue = (Color.blue(original) * inverse + Color.blue(newColor) * coverage).toInt()
            resultPixels[index] = Color.argb(Color.alpha(original), red, green, blue)
            changed = true
        }

        mask.recycle()
        if (!changed) return null
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        result.setPixels(resultPixels, 0, regionWidth, left, top, regionWidth, regionHeight)
        return result
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxSide: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / largest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return if (scaled.config == Bitmap.Config.ARGB_8888 && scaled.isMutable) {
            scaled
        } else {
            scaled.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun isDark(color: Int, threshold: Int): Boolean {
        val luminance = (
            0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)
            ).toInt()
        return luminance < threshold
    }

    private fun colorsClose(first: Int, second: Int, tolerance: Int): Boolean =
        kotlin.math.abs(Color.red(first) - Color.red(second)) <= tolerance &&
            kotlin.math.abs(Color.green(first) - Color.green(second)) <= tolerance &&
            kotlin.math.abs(Color.blue(first) - Color.blue(second)) <= tolerance
}
