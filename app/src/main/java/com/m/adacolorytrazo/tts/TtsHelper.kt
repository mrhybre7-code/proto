package com.m.adacolorytrazo.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/** La frase pendiente se reproduce cuando el motor confirma que está listo. */
object TtsHelper {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var generation = 0

    fun init(ctx: Context) {
        if (tts != null) return
        val token = ++generation
        tts = TextToSpeech(ctx.applicationContext) { status ->
            // Algunos motores notifican durante su constructor. Ejecutar luego evita esa carrera.
            mainHandler.post {
                if (token != generation) return@post
                val engine = tts ?: return@post
                ready = false
                if (status == TextToSpeech.SUCCESS) {
                    val locales = listOf(Locale("es", "CL"), Locale("es", "ES"), Locale("es"))
                    for (locale in locales) {
                        if (engine.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE) {
                            ready = true
                            break
                        }
                    }
                    if (ready) {
                        engine.setPitch(1.05f)
                        engine.setSpeechRate(0.98f)
                        val text = pendingText
                        pendingText = null
                        if (text != null) speak(text)
                    }
                }
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pendingText = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ADA_TTS_${System.nanoTime()}")
    }

    fun shutdown() {
        generation++
        ready = false
        pendingText = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
