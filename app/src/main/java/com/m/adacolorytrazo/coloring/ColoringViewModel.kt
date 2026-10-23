package com.m.adacolorytrazo.coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m.adacolorytrazo.util.BitmapUtils
import com.m.adacolorytrazo.util.ImageSaveResult
import com.m.adacolorytrazo.util.ImageSaver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ColoringUiState(
    val bitmap: Bitmap? = null,
    val selectedColor: Int = DEFAULT_COLOR,
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

private const val MAX_SNAPSHOTS = 8
private const val HISTORY_BYTES = 24L * 1024 * 1024
private const val DEFAULT_COLOR = 0xFFE53935.toInt()

/** Un único editor activo evita acumular un ViewModel y su historial por cada recurso. */
class ColoringViewModel : ViewModel() {
    var state by mutableStateOf(ColoringUiState())
        private set
    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()
    private var initializedResource: Int? = null
    private var activeJob: Job? = null
    private var generation = 0

    fun initialize(context: Context, resourceId: Int) {
        if (initializedResource == resourceId && (state.bitmap != null || state.isLoading)) return
        load(context, resourceId, reset = true)
    }

    private fun load(context: Context, resourceId: Int, reset: Boolean) {
        cancelWork()
        val token = generation
        initializedResource = resourceId
        if (reset) {
            undoStack.clear()
            redoStack.clear()
            state = ColoringUiState(selectedColor = state.selectedColor, isLoading = true, isBusy = true)
        }
        val appContext = context.applicationContext
        activeJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapUtils.decodeResourceMutable(appContext, resourceId)
                }
                if (token != generation) return@launch
                state = ColoringUiState(bitmap = bitmap, selectedColor = state.selectedColor)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (token == generation) {
                    state = state.copy(error = failure.message ?: "No se pudo cargar el dibujo")
                }
            } finally {
                if (token == generation) {
                    state = state.copy(isBusy = false, isLoading = false)
                    activeJob = null
                }
            }
        }
    }

    fun cancelWork() {
        generation++
        activeJob?.cancel()
        activeJob = null
        state = state.copy(isBusy = false, isLoading = false)
    }

    fun selectColor(color: Int) {
        if (!state.isBusy) state = state.copy(selectedColor = color)
    }

    fun fillAt(x: Int, y: Int) {
        val source = state.bitmap ?: return
        if (state.isBusy) return
        val color = state.selectedColor
        val token = generation
        state = state.copy(isBusy = true, error = null)
        activeJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    BitmapUtils.floodFill(source, x, y, color)
                }
                if (token != generation) return@launch
                if (result != null) {
                    redoStack.clear()
                    addUndo(source)
                    state = state.copy(
                        bitmap = result,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (token == generation) {
                    state = state.copy(error = failure.message ?: "No se pudo rellenar esa zona")
                }
            } finally {
                if (token == generation) {
                    state = state.copy(isBusy = false)
                    activeJob = null
                }
            }
        }
    }

    fun paintStroke(points: List<PointF>, strokeWidth: Float) {
        val source = state.bitmap ?: return
        if (state.isBusy || points.isEmpty() || strokeWidth <= 0f) return
        val color = state.selectedColor
        val token = generation
        state = state.copy(isBusy = true, error = null)
        activeJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    BitmapUtils.paintStroke(source, points, color, strokeWidth)
                }
                if (token != generation) return@launch
                if (result != null) {
                    redoStack.clear()
                    addUndo(source)
                    state = state.copy(
                        bitmap = result,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (token == generation) {
                    state = state.copy(error = failure.message ?: "No se pudo pintar ese trazo")
                }
            } finally {
                if (token == generation) {
                    state = state.copy(isBusy = false)
                    activeJob = null
                }
            }
        }
    }

    fun reset(context: Context, resourceId: Int) {
        if (!state.isBusy) load(context, resourceId, reset = true)
    }

    fun undo() {
        if (state.isBusy || undoStack.isEmpty()) return
        val current = state.bitmap ?: return
        redoStack.addLast(current)
        state = state.copy(
            bitmap = undoStack.removeLast(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = true
        )
    }

    fun redo() {
        if (state.isBusy || redoStack.isEmpty()) return
        val current = state.bitmap ?: return
        val next = redoStack.removeLast()
        addUndo(current)
        state = state.copy(bitmap = next, canUndo = true, canRedo = redoStack.isNotEmpty())
    }

    suspend fun save(context: Context, filename: String): ImageSaveResult {
        val current = state.bitmap ?: return ImageSaveResult.Failure("El dibujo todavía se está cargando")
        if (state.isBusy) return ImageSaveResult.Failure("El dibujo está ocupado")
        val token = generation
        state = state.copy(isBusy = true, error = null)
        return try {
            ImageSaver.saveToPictures(context.applicationContext, current, filename)
        } finally {
            if (token == generation) state = state.copy(isBusy = false)
        }
    }

    private fun addUndo(snapshot: Bitmap) {
        undoStack.addLast(snapshot)
        while (undoStack.isNotEmpty() &&
            (undoStack.size > MAX_SNAPSHOTS ||
                undoStack.sumOf { it.allocationByteCount.toLong() } +
                redoStack.sumOf { it.allocationByteCount.toLong() } > HISTORY_BYTES)
        ) {
            undoStack.removeFirst()
        }
        // Los descartados quedan para el GC; no reciclar un bitmap aún usado por el renderizador.
    }

    override fun onCleared() {
        cancelWork()
        undoStack.clear()
        redoStack.clear()
        state = ColoringUiState()
        super.onCleared()
    }
}
