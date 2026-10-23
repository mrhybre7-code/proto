package com.m.adacolorytrazo.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import com.m.adacolorytrazo.util.ImageSaveResult
import com.m.adacolorytrazo.util.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_RENDER_SIDE = 1536
private const val MIN_POINT_DISTANCE = 2.5f

data class FreeDrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean
)

class FreeDrawingViewModel : ViewModel() {
    val strokes = mutableStateListOf<FreeDrawingStroke>()
    private val undone = mutableStateListOf<FreeDrawingStroke>()

    var currentColor by mutableStateOf(Color.Black)
        private set
    var currentWidth by mutableFloatStateOf(10f)
        private set
    var eraser by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set

    val canUndo: Boolean
        get() = strokes.isNotEmpty()
    val canRedo: Boolean
        get() = undone.isNotEmpty()

    private var activeStrokeIndex by mutableStateOf<Int?>(null)
    private var canvasSize by mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1))

    fun selectColor(color: Color) {
        if (!isSaving) {
            currentColor = color
            eraser = false
        }
    }

    fun setWidth(width: Float) {
        if (!isSaving) currentWidth = width
    }

    fun toggleEraser() {
        if (!isSaving) eraser = !eraser
    }

    fun beginStroke(point: Offset) {
        if (isSaving) return
        undone.clear()
        strokes.add(
            FreeDrawingStroke(
                points = listOf(point),
                color = currentColor,
                width = currentWidth,
                isEraser = eraser
            )
        )
        activeStrokeIndex = strokes.lastIndex
    }

    /** Reemplaza el stroke en la lista para que Compose observe cada punto. */
    fun appendPoint(point: Offset) {
        if (isSaving) return
        val index = activeStrokeIndex ?: return
        val stroke = strokes.getOrNull(index) ?: return
        val last = stroke.points.lastOrNull()
        if (last != null && distance(last, point) < MIN_POINT_DISTANCE) return
        strokes[index] = stroke.copy(points = stroke.points + point)
    }

    fun endStroke() {
        activeStrokeIndex = null
    }

    fun cancelStroke() {
        activeStrokeIndex?.let { index ->
            if (index == strokes.lastIndex) strokes.removeAt(index)
        }
        activeStrokeIndex = null
    }

    fun undo() {
        if (isSaving || activeStrokeIndex != null || strokes.isEmpty()) return
        undone.add(strokes.removeAt(strokes.lastIndex))
    }

    fun redo() {
        if (isSaving || activeStrokeIndex != null || undone.isEmpty()) return
        strokes.add(undone.removeAt(undone.lastIndex))
    }

    fun clear() {
        if (isSaving) return
        strokes.clear()
        undone.clear()
        activeStrokeIndex = null
    }

    /** Conserva el dibujo visible cuando la actividad cambia de tamaño u orientación. */
    fun resize(newSize: androidx.compose.ui.unit.IntSize) {
        val old = canvasSize
        if (newSize == old) return
        if (old.width > 1 && old.height > 1 && newSize.width > 0 && newSize.height > 0) {
            val sx = newSize.width.toFloat() / old.width
            val sy = newSize.height.toFloat() / old.height
            fun scale(stroke: FreeDrawingStroke) = stroke.copy(
                points = stroke.points.map { Offset(it.x * sx, it.y * sy) },
                width = stroke.width * min(sx, sy)
            )
            for (index in strokes.indices) strokes[index] = scale(strokes[index])
            for (index in undone.indices) undone[index] = scale(undone[index])
        }
        canvasSize = newSize
    }

    suspend fun save(context: android.content.Context, filename: String): ImageSaveResult {
        if (isSaving) return ImageSaveResult.Failure("El dibujo está ocupado")
        val width = canvasSize.width.coerceAtLeast(1)
        val height = canvasSize.height.coerceAtLeast(1)
        val snapshot = strokes.toList()
        isSaving = true
        var bitmap: Bitmap? = null
        return try {
            bitmap = withContext(Dispatchers.Default) {
                renderStrokesToBitmap(width, height, snapshot)
            }
            ImageSaver.saveToPictures(context.applicationContext, bitmap, filename)
        } finally {
            bitmap?.recycle()
            isSaving = false
        }
    }
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

/** Renderiza fuera de la UI y reduce el PNG si el lienzo fuese excepcionalmente grande. */
private fun renderStrokesToBitmap(
    widthPx: Int,
    heightPx: Int,
    strokes: List<FreeDrawingStroke>
): Bitmap {
    val scale = min(1f, MAX_RENDER_SIDE.toFloat() / maxOf(widthPx, heightPx))
    val width = (widthPx * scale).roundToInt().coerceAtLeast(1)
    val height = (heightPx * scale).roundToInt().coerceAtLeast(1)
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.scale(scale, scale)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    strokes.forEach { stroke ->
        paint.strokeWidth = stroke.width
        paint.color = if (stroke.isEraser) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.argb(
                (stroke.color.alpha * 255).roundToInt(),
                (stroke.color.red * 255).roundToInt(),
                (stroke.color.green * 255).roundToInt(),
                (stroke.color.blue * 255).roundToInt()
            )
        }
        if (stroke.points.size >= 2) {
            val path = AndroidPath().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                for (point in stroke.points.drop(1)) lineTo(point.x, point.y)
            }
            canvas.drawPath(path, paint)
        } else if (stroke.points.size == 1) {
            val point = stroke.points.first()
            val oldStyle = paint.style
            paint.style = Paint.Style.FILL
            canvas.drawCircle(point.x, point.y, stroke.width / 2f, paint)
            paint.style = oldStyle
        }
    }
    return bitmap
}
