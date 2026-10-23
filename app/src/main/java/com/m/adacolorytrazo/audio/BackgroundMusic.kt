package com.m.adacolorytrazo.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.m.adacolorytrazo.R

object BackgroundMusic {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack = 1

    fun start(context: Context) {
        if (mediaPlayer != null) return
        val resId = if (currentTrack == 1) R.raw.fondo1 else R.raw.fondo2
        var player: MediaPlayer? = null
        try {
            player = MediaPlayer.create(context.applicationContext, resId) ?: return
            player.isLooping = true
            player.setOnErrorListener { _, what, extra ->
                Log.w("AdaMusic", "No se pudo reproducir la música: $what/$extra")
                stop()
                true
            }
            player.start()
            mediaPlayer = player
        } catch (e: RuntimeException) {
            player?.release()
            mediaPlayer = null
            Log.w("AdaMusic", "No se pudo iniciar la música", e)
        }
    }

    fun switchTrack(context: Context) {
        currentTrack = if (currentTrack == 1) 2 else 1
        stop()
        start(context)
    }

    fun isStarted(): Boolean = mediaPlayer != null

    fun stop() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) player.stop()
        } catch (e: IllegalStateException) {
            Log.w("AdaMusic", "El reproductor ya estaba detenido", e)
        } finally {
            player.release()
        }
    }
}
