package com.m.adacolorytrazo.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.m.adacolorytrazo.R
import java.util.ArrayDeque
import java.util.Locale
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class SoundHelper(context: Context) {
    private var soundPool: SoundPool? = null
    private var coinId: Int = 0

    init {
        val appContext = context.applicationContext

        runCatching {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            coinId = soundPool?.load(
                appContext,
                R.raw.coin,
                1
            ) ?: 0
        }.onFailure {
            soundPool = null
            coinId = 0
        }
    }

    fun playCoin() {
        val pool = soundPool ?: return

        if (coinId != 0) {
            pool.play(
                coinId,
                1f,
                1f,
                1,
                0,
                1f
            )
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

@Composable
fun rememberSound(): SoundHelper {
    val context = LocalContext.current.applicationContext

    val helper = remember {
        SoundHelper(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.release()
        }
    }

    return helper
}

class Haptics(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        ContextCompat.getSystemService(
            context.applicationContext,
            Vibrator::class.java
        )
    }.getOrNull()

    fun reward() {
        val currentVibrator = vibrator ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentVibrator.vibrate(
                VibrationEffect.createOneShot(
                    60L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            currentVibrator.vibrate(60L)
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current.applicationContext

    return remember {
        Haptics(context)
    }
}

class SpeechHelper(context: Context) :
    TextToSpeech.OnInitListener {

    private val stateLock = Any()
    private val pendingTexts = ArrayDeque<String>()
    private var initialized = false
    private var released = false
    private val _ready = mutableStateOf(false)

    /** Observable para quien necesite distinguir motor listo de texto visible. */
    val ready: State<Boolean>
        get() = _ready

    private val textToSpeech = TextToSpeech(
        context.applicationContext,
        this
    )

    override fun onInit(status: Int) {
        synchronized(stateLock) {
            if (released) return

            initialized = true
            if (status != TextToSpeech.SUCCESS) {
                pendingTexts.clear()
                _ready.value = false
                return
            }

            val languageReady = configureAvailableLanguage()
            _ready.value = languageReady

            if (!languageReady) {
                // No conservar mensajes indefinidamente si el motor no tiene idioma.
                pendingTexts.clear()
                return
            }

            textToSpeech.setSpeechRate(0.9f)
            textToSpeech.setPitch(1.05f)

            val queued = pendingTexts.toList()
            pendingTexts.clear()
            queued.forEachIndexed { index, text ->
                speakNow(text, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD)
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return

        synchronized(stateLock) {
            if (released) return

            if (!initialized || !_ready.value) {
                // OnInit es asíncrono: no perder el primer objetivo anunciado.
                if (pendingTexts.size >= 8) pendingTexts.removeFirst()
                pendingTexts.addLast(text)
            } else {
                speakNow(text, TextToSpeech.QUEUE_FLUSH)
            }
        }
    }

    private fun configureAvailableLanguage(): Boolean {
        val candidates = listOf(
            Locale("es", "CL"),
            Locale("es", "ES"),
            Locale("es", "MX"),
            Locale("es"),
            Locale.getDefault()
        ).distinct()

        for (locale in candidates) {
            val availability = textToSpeech.isLanguageAvailable(locale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                val result = textToSpeech.setLanguage(locale)
                if (result >= TextToSpeech.LANG_AVAILABLE) return true
            }
        }
        return false
    }

    private fun speakNow(text: String, queueMode: Int) {
        textToSpeech.speak(
            text,
            queueMode,
            Bundle(),
            "ada_feedback"
        )
    }

    private fun stop() {
        textToSpeech.stop()
    }

    fun release() {
        synchronized(stateLock) {
            if (released) return
            released = true
            initialized = false
            _ready.value = false
            pendingTexts.clear()
            stop()
            textToSpeech.shutdown()
        }
    }
}

@Composable
fun rememberSpeech(): SpeechHelper {
    val context = LocalContext.current.applicationContext

    val helper = remember {
        SpeechHelper(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.release()
        }
    }

    return helper
}

data class ConfettiParticle(
    val position: Offset,
    val velocity: Offset,
    val life: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    visible: Boolean,
    burstKey: Int,
    durationMs: Long = 1200L
) {
    if (!visible) return

    var particles by remember(burstKey) {
        mutableStateOf(emptyList<ConfettiParticle>())
    }

    LaunchedEffect(burstKey) {
        val colors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFC857),
            Color(0xFF4E60CE),
            Color(0xFF2DC653),
            Color(0xFFFF9F1C),
            Color(0xFF7F3FBF),
            Color(0xFFFF8FAB),
            Color(0xFF4EC5F1)
        )

        val random = Random(burstKey)

        particles = List(90) {
            val angle = random.nextDouble(
                0.0,
                Math.PI * 2
            )

            val speed = random.nextDouble(
                150.0,
                420.0
            ).toFloat()

            val velocityX = (
                    cos(angle) * speed
                    ).toFloat()

            val velocityY = (
                    sin(angle) * speed
                    ).toFloat()

            ConfettiParticle(
                position = Offset.Zero,
                velocity = Offset(
                    velocityX,
                    velocityY - 120f
                ),
                life = 1f,
                color = colors.random(random),
                size = random.nextDouble(
                    5.0,
                    10.0
                ).toFloat()
            )
        }

        val durationNanos = durationMs.coerceAtLeast(1L) * 1_000_000L
        val startNanos = withFrameNanos { it }
        var lastNanos = startNanos

        try {
            while (isActive) {
                val nowNanos = withFrameNanos { it }
                val deltaTime = (
                    (nowNanos - lastNanos).coerceAtMost(32_000_000L)
                ).toFloat() / 1_000_000_000f
                lastNanos = nowNanos

                val elapsedNanos = (nowNanos - startNanos).coerceAtLeast(0L)
                val progress = (elapsedNanos.toFloat() / durationNanos).coerceIn(0f, 1f)
                val gravity = 480f

                particles = particles
                    .map { particle ->
                        val newVelocity = Offset(
                            particle.velocity.x,
                            particle.velocity.y + gravity * deltaTime
                        )

                        val newPosition = Offset(
                            particle.position.x + newVelocity.x * deltaTime,
                            particle.position.y + newVelocity.y * deltaTime
                        )

                        particle.copy(
                            position = newPosition,
                            velocity = newVelocity,
                            life = max(0f, 1f - progress)
                        )
                    }
                    .filter { particle -> particle.life > 0.01f }

                if (elapsedNanos >= durationNanos || particles.isEmpty()) break
            }
        } finally {
            // LaunchedEffect se cancela al cambiar de burst/visible: nunca dejar estado viejo.
            particles = emptyList()
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        if (particles.isEmpty()) {
            return@Canvas
        }

        val centerX = size.width / 2f
        val centerY = size.height / 3f

        particles.forEach { particle ->
            val alpha = particle.life.coerceIn(0f, 1f)

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(
                    centerX + particle.position.x,
                    centerY + particle.position.y
                )
            )
        }
    }
}
