package com.m.adacolorytrazo.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/** Resultado explícito de un guardado; la UI decide cómo comunicarlo al usuario. */
sealed interface ImageSaveResult {
    data object Success : ImageSaveResult
    data class Failure(val reason: String) : ImageSaveResult
}

/**
 * Guardado único para las dos pantallas de dibujo.
 *
 * Todo el trabajo de archivo/MediaStore ocurre en IO. Una entrada pendiente y el
 * archivo legado se eliminan si la compresión, el stream, el commit o la
 * corrutina fallan (la cancelación se vuelve a lanzar para respetar structured
 * concurrency).
 */
object ImageSaver {
    private const val MIME_PNG = "image/png"
    private const val RELATIVE_DIRECTORY = "Pictures/AdaColorYTrazo"

    suspend fun saveToPictures(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): ImageSaveResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val resolver = appContext.contentResolver
        var uri: Uri? = null
        var legacyFile: File? = null

        try {
            ensureActive()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, MIME_PNG)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIRECTORY)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext ImageSaveResult.Failure("No se pudo crear la imagen")

                val output = resolver.openOutputStream(uri!!)
                    ?: throw IllegalStateException("No se pudo abrir el archivo")
                output.use { stream ->
                    ensureActive()
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IllegalStateException("La compresión PNG falló")
                    }
                    stream.flush()
                }
                ensureActive()

                val committed = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                if (resolver.update(uri!!, committed, null, null) != 1) {
                    throw IllegalStateException("No se pudo confirmar la imagen")
                }
                uri = null
                ImageSaveResult.Success
            } else {
                val pictures = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val directory = File(pictures, "AdaColorYTrazo")
                if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
                    throw IllegalStateException("No se pudo crear la carpeta de imágenes")
                }
                legacyFile = File(directory, filename)
                legacyFile!!.outputStream().use { stream ->
                    ensureActive()
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IllegalStateException("La compresión PNG falló")
                    }
                    stream.flush()
                }
                ensureActive()

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, legacyFile!!.absolutePath)
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                }
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("La galería no aceptó la imagen")
                legacyFile = null
                uri = null
                ImageSaveResult.Success
            }
        } catch (cancelled: CancellationException) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            legacyFile?.let { runCatching { it.delete() } }
            throw cancelled
        } catch (failure: Throwable) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            legacyFile?.let { runCatching { it.delete() } }
            ImageSaveResult.Failure(failure.message ?: "No se pudo guardar la imagen")
        }
    }
}
