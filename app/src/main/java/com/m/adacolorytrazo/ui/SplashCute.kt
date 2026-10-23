package com.m.adacolorytrazo.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.m.adacolorytrazo.R
import kotlin.math.max
import kotlin.math.min

/**
 * Intro offline: R.raw.ada_intro_banca contains the pre-rendered 3D-look clip.
 * There is deliberately no TTS call here; the MP4 carries the greeting audio.
 */
@Composable
fun SplashCute(onFinish: () -> Unit) {
    val context = LocalContext.current
    val composeView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFinish = rememberUpdatedState(onFinish)
    var savedPositionMs by rememberSaveable { mutableIntStateOf(0) }
    var playbackError by rememberSaveable { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // A stable callback object lets the view outlive recompositions without keeping
    // an obsolete onFinish lambda.
    val finishSplash = remember {
        {
            if (!finished) {
                finished = true
                latestOnFinish.value()
            }
        }
    }

    val videoHolder = remember { IntroVideoHolder() }

    DisposableEffect(composeView) {
        val activity = context.findActivity()
        val controller = activity?.let { WindowInsetsControllerCompat(it.window, composeView) }
        val previousBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousBehavior != null) controller?.systemBarsBehavior = previousBehavior
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ada_intro_banca_poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                IntroVideoView(
                    context = viewContext,
                    initialPositionMs = savedPositionMs,
                    initialError = playbackError,
                    onPositionChanged = { savedPositionMs = it },
                    onFinished = finishSplash,
                    onPlaybackError = { playbackError = true }
                ).also { videoHolder.view = it }
            },
            update = { /* All mutable playback state is owned by IntroVideoView. */ }
        )

        // Put the fallback above the TextureView: a vendor may leave its dead
        // surface black after an error, which must not hide the poster.
        if (playbackError) {
            Image(
                painter = painterResource(R.drawable.ada_intro_banca_poster),
                contentDescription = "Ada en la banca con sus animales",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = finishSplash,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            enabled = !finished
        ) {
            Text(if (playbackError) "Usar menú" else "Entrar a jugar")
        }

        // The button is intentionally present from the first frame, not a delayed
        // overlay. Android Button supplies keyboard, switch-access, and TalkBack
        // semantics without adding another dependency.
    }

    // ON_PAUSE saves the position even when the activity is not destroyed; a
    // destroyed TextureView also saves/releases in onSurfaceTextureDestroyed.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> videoHolder.view?.setLifecycleResumed(false)
                Lifecycle.Event.ON_RESUME -> videoHolder.view?.setLifecycleResumed(true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            videoHolder.view?.setLifecycleResumed(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoHolder.view?.dispose()
            videoHolder.view = null
        }
    }
}

/**
 * TextureView host. It stays independent of Compose layout state so a rotation
 * can tear down/recreate the Surface while retaining the current millisecond.
 */
private class IntroVideoHolder {
    var view: IntroVideoView? = null
}

private class IntroVideoView(
    context: Context,
    private val initialPositionMs: Int,
    initialError: Boolean,
    private val onPositionChanged: (Int) -> Unit,
    private val onFinished: () -> Unit,
    private val onPlaybackError: () -> Unit
) : FrameLayout(context) {

    private val viewContext: Context = context
    private val textureView = TextureView(viewContext)
    private var mediaPlayer: MediaPlayer? = null
    private var outputSurface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var resumePositionMs = initialPositionMs.coerceAtLeast(0)
    private var lifecycleResumed = false
    private var playbackError = initialError
    private var completionSignalled = false
    private var disposed = false

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            if (disposed) return
            viewportWidth = width
            viewportHeight = height
            releaseOutputSurface()
            outputSurface = Surface(surfaceTexture)
            ensurePlayer()
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            if (disposed) return
            viewportWidth = width
            viewportHeight = height
            updateVideoTransform()
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            pauseAndRememberPosition()
            // Release the player before the Surface wrapper; this avoids leaving
            // MediaPlayer attached to a dead Surface during rotation/backgrounding.
            releasePlayer()
            releaseOutputSurface()
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    init {
        // El video vertical llena el teléfono; en landscape las bandas son oscuras,
        // sin repetir un poster estirado detrás de la escena.
        setBackgroundColor(android.graphics.Color.rgb(24, 54, 91))
        textureView.isOpaque = true
        textureView.surfaceTextureListener = surfaceListener
        addView(
            textureView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun setLifecycleResumed(resumed: Boolean) {
        if (disposed) return
        lifecycleResumed = resumed
        if (!resumed) {
            pauseAndRememberPosition()
            return
        }
        if (playbackError || completionSignalled) return
        ensurePlayer()
        val player = mediaPlayer ?: return
        try {
            if (!player.isPlaying) player.start()
        } catch (_: IllegalStateException) {
            failPlayback()
        } catch (_: RuntimeException) {
            failPlayback()
        }
    }

    fun dispose() {
        if (disposed) return
        // Keep this before the releases: configuration change needs the latest
        // currentPosition, and all callbacks become no-ops after disposed=true.
        pauseAndRememberPosition()
        disposed = true
        textureView.surfaceTextureListener = null
        releasePlayer()
        releaseOutputSurface()
    }

    private fun ensurePlayer() {
        if (disposed || playbackError || outputSurface == null || mediaPlayer != null) return

        val player = try {
            MediaPlayer.create(viewContext.applicationContext, R.raw.ada_intro_banca)
        } catch (_: RuntimeException) {
            null
        }

        if (player == null) {
            failPlayback()
            return
        }
        if (disposed || playbackError) {
            player.release()
            return
        }

        mediaPlayer = player
        try {
            player.setOnCompletionListener { completedPlayer ->
                if (
                    disposed ||
                    playbackError ||
                    completionSignalled ||
                    completedPlayer !== mediaPlayer
                ) return@setOnCompletionListener
                completionSignalled = true
                rememberPosition(player)
                onFinished()
            }
            player.setOnErrorListener { errorPlayer, _, _ ->
                if (!disposed && errorPlayer === mediaPlayer) failPlayback()
                true
            }
            player.setOnVideoSizeChangedListener { sizedPlayer, width, height ->
                if (!disposed && sizedPlayer === mediaPlayer) {
                    videoWidth = width
                    videoHeight = height
                    updateVideoTransform()
                }
            }
            player.setSurface(outputSurface)
            videoWidth = player.videoWidth
            videoHeight = player.videoHeight
            updateVideoTransform()

            val durationMs = player.duration.coerceAtLeast(0)
            val seekMs = resumePositionMs.coerceIn(0, durationMs)
            if (seekMs > 0) player.seekTo(seekMs)

            if (lifecycleResumed && !completionSignalled) player.start()
        } catch (_: IllegalStateException) {
            failPlayback()
        } catch (_: RuntimeException) {
            failPlayback()
        }
    }

    private fun pauseAndRememberPosition() {
        val player = mediaPlayer ?: return
        rememberPosition(player)
        try {
            if (player.isPlaying) player.pause()
        } catch (_: IllegalStateException) {
            // releasePlayer() below remains the cleanup path for an invalid state.
        } catch (_: RuntimeException) {
            // Some vendor MediaPlayer implementations throw RuntimeException here.
        }
    }

    private fun rememberPosition(player: MediaPlayer) {
        if (disposed) return
        try {
            resumePositionMs = player.currentPosition.coerceAtLeast(0)
            onPositionChanged(resumePositionMs)
        } catch (_: IllegalStateException) {
            // The callback can race a vendor teardown; the next saved value wins.
        } catch (_: RuntimeException) {
            // Do not let a vendor player state prevent resource cleanup/navigation.
        }
    }

    private fun failPlayback() {
        if (disposed || playbackError) return
        playbackError = true
        pauseAndRememberPosition()
        releasePlayer()
        releaseOutputSurface()
        onPlaybackError()
    }

    private fun releasePlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            player.setOnVideoSizeChangedListener(null)
        } catch (_: RuntimeException) {
            // Listener removal is best effort; release is still mandatory.
        }
        try {
            player.release()
        } catch (_: RuntimeException) {
            // MediaPlayer.release() is documented as terminal; vendor exceptions
            // must not stop Surface cleanup.
        }
    }

    private fun releaseOutputSurface() {
        val surface = outputSurface ?: return
        outputSurface = null
        try {
            surface.release()
        } catch (_: RuntimeException) {
            // Already-released Surface is harmless; keep cleanup idempotent.
        }
    }

    private fun updateVideoTransform() {
        if (disposed || viewportWidth <= 0 || viewportHeight <= 0) return
        if (videoWidth <= 0 || videoHeight <= 0) return

        val widthScale = viewportWidth.toFloat() / videoWidth.toFloat()
        val heightScale = viewportHeight.toFloat() / videoHeight.toFloat()
        // Portrait phones use centerCrop. Landscape uses fit so the entire scene
        // remains visible, accepting letterbox bars when aspect ratios differ.
        val scale = if (viewportWidth < viewportHeight) {
            max(widthScale, heightScale)
        } else {
            min(widthScale, heightScale)
        }
        val scaledWidth = videoWidth * scale
        val scaledHeight = videoHeight * scale
        val dx = (viewportWidth - scaledWidth) / 2f
        val dy = (viewportHeight - scaledHeight) / 2f

        val matrix = Matrix().apply {
            // TextureView already stretches decoder pixels to its own bounds.
            // Transform those viewport coordinates, not the original video pixels.
            setScale(scaledWidth / viewportWidth, scaledHeight / viewportHeight)
            postTranslate(dx, dy)
        }
        textureView.setTransform(matrix)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
