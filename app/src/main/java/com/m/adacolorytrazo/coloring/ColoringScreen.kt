package com.m.adacolorytrazo.coloring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.PointF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m.adacolorytrazo.R
import com.m.adacolorytrazo.ui.PlayfulGameBackdrop
import com.m.adacolorytrazo.util.ImageSaveResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private data class PaletteColor(val value: Int, val name: String)

private val COLORING_PALETTE = listOf(
    PaletteColor(android.graphics.Color.BLACK, "negro"),
    PaletteColor(android.graphics.Color.DKGRAY, "gris oscuro"),
    PaletteColor(android.graphics.Color.LTGRAY, "gris claro"),
    PaletteColor(android.graphics.Color.WHITE, "blanco"),
    PaletteColor("#E53935".toColorInt(), "rojo"),
    PaletteColor("#FB8C00".toColorInt(), "naranja"),
    PaletteColor("#FDD835".toColorInt(), "amarillo"),
    PaletteColor("#43A047".toColorInt(), "verde"),
    PaletteColor("#1E88E5".toColorInt(), "azul"),
    PaletteColor("#8E24AA".toColorInt(), "violeta"),
    PaletteColor("#F06292".toColorInt(), "rosado"),
    PaletteColor("#795548".toColorInt(), "café")
)

private val COLORING_BRUSH_SIZES = listOf(6, 14, 24, 34)

@Suppress("DiscouragedApi")
@Composable
fun ColoringScreen(
    baseName: String,
    title: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val bwRes = remember(baseName) {
        context.resources.getIdentifier("${baseName}_bw", "drawable", context.packageName)
    }

    if (bwRes == 0) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("No se encontró el dibujo: ${baseName}_bw", color = Color.Red)
        }
        return
    }

    val model: ColoringViewModel = viewModel(key = "coloring-editor")
    val ui = model.state
    var imageBoxSize by remember { mutableStateOf(IntSize.Zero) }
    var brushMode by rememberSaveable { mutableStateOf(true) }
    var brushSizeDp by rememberSaveable { mutableIntStateOf(14) }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var touchFeedbackKey by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    DisposableEffect(model, baseName) {
        onDispose { model.cancelWork() }
    }
    LaunchedEffect(baseName, bwRes) {
        model.initialize(context.applicationContext, bwRes)
    }
    LaunchedEffect(ui.error) {
        ui.error?.let { snackbarHost.showSnackbar(it) }
    }

    fun performSave() {
        scope.launch {
            try {
                when (val result = model.save(
                    context.applicationContext,
                    "${baseName}_${System.currentTimeMillis()}.png"
                )) {
                    ImageSaveResult.Success -> snackbarHost.showSnackbar("Dibujo guardado en la galería")
                    is ImageSaveResult.Failure -> snackbarHost.showSnackbar("No se pudo guardar: ${result.reason}")
                }
            } catch (_: CancellationException) {
                // Una rotación o salida cancela la operación y ImageSaver ya limpió su URI.
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performSave()
        } else {
            scope.launch {
                snackbarHost.showSnackbar("Se necesita permiso de almacenamiento para guardar")
            }
        }
    }

    fun requestSave() {
        if (ui.isBusy) return
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            performSave()
        }
    }

    Box(Modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHost) }
        ) { padding ->
            val bitmap = ui.bitmap
            if (bitmap == null) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.isLoading) CircularProgressIndicator()
                    else Text(ui.error ?: "Cargando dibujo…")
                }
            } else {
                val bitmapWidth = bitmap.width
                val bitmapHeight = bitmap.height
                val drawScale = if (imageBoxSize.width > 0 && imageBoxSize.height > 0) {
                    min(
                        imageBoxSize.width.toFloat() / bitmapWidth,
                        imageBoxSize.height.toFloat() / bitmapHeight
                    )
                } else 0f
                val brushWidthPx = with(density) { brushSizeDp.dp.toPx() }
                val brushWidthBitmap = if (drawScale > 0f) brushWidthPx / drawScale else 0f
                Column(
                    Modifier.fillMaxSize().padding(padding)
                        .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (baseName == "barco") ColoringBoatCompanion()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.88f),
                        shadowElevation = 6.dp,
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.95f))
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(78.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            items(COLORING_PALETTE, key = { it.name }) { color ->
                                ColoringPaletteSwatch(
                                    color = color,
                                    selected = ui.selectedColor == color.value,
                                    enabled = !ui.isBusy,
                                    onClick = { model.selectColor(color.value) }
                                )
                            }
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(66.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(COLORING_BRUSH_SIZES, key = { it }) { widthDp ->
                            ColoringWidthButton(
                                widthDp = widthDp,
                                selected = brushSizeDp == widthDp,
                                enabled = !ui.isBusy,
                                onClick = { brushSizeDp = widthDp }
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            ColoringToolButton(
                                icon = { Icon(Icons.Filled.Brush, contentDescription = null) },
                                description = "Pincel",
                                selected = brushMode,
                                enabled = !ui.isBusy,
                                onClick = { brushMode = true }
                            )
                        }
                        item {
                            ColoringToolButton(
                                icon = { Icon(Icons.Filled.FormatColorFill, contentDescription = null) },
                                description = "Cubeta",
                                selected = !brushMode,
                                enabled = !ui.isBusy,
                                onClick = { brushMode = false }
                            )
                        }
                        item {
                            ColoringToolButton(
                                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                description = "Limpiar dibujo",
                                selected = false,
                                enabled = !ui.isBusy,
                                onClick = { model.reset(context.applicationContext, bwRes) }
                            )
                        }
                        item {
                            ColoringToolButton(
                                icon = { Icon(Icons.Filled.Undo, contentDescription = null) },
                                description = "Deshacer",
                                selected = false,
                                enabled = !ui.isBusy && ui.canUndo,
                                onClick = model::undo
                            )
                        }
                        item {
                            ColoringToolButton(
                                icon = { Icon(Icons.Filled.Redo, contentDescription = null) },
                                description = "Rehacer",
                                selected = false,
                                enabled = !ui.isBusy && ui.canRedo,
                                onClick = model::redo
                            )
                        }
                        item {
                            ColoringToolButton(
                                icon = {
                                    if (ui.isBusy) CircularProgressIndicator(Modifier.size(27.dp))
                                    else Icon(Icons.Filled.Save, contentDescription = null)
                                },
                                description = "Guardar dibujo",
                                selected = false,
                                enabled = !ui.isBusy,
                                onClick = ::requestSave
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmapWidth.toFloat() / bitmapHeight.toFloat())
                            .shadow(14.dp, RoundedCornerShape(26.dp), clip = false)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White)
                            .border(4.dp, Color.White.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
                            .onGloballyPositioned { imageBoxSize = it.size }
                            .pointerInput(brushMode, ui.selectedColor, ui.isBusy, brushSizeDp, bitmap, imageBoxSize) {
                                if (ui.isBusy || drawScale <= 0f) return@pointerInput
                                val drawW = bitmapWidth * drawScale
                                val drawH = bitmapHeight * drawScale
                                val dx = (imageBoxSize.width - drawW) / 2f
                                val dy = (imageBoxSize.height - drawH) / 2f

                                if (brushMode) {
                                    detectDragGestures(
                                        onDragStart = {
                                            activeStroke = listOf(it)
                                            touchPoint = it
                                            touchFeedbackKey++
                                        },
                                        onDrag = { change, _ ->
                                            activeStroke = activeStroke + change.position
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            val bitmapPoints = activeStroke.map { point ->
                                                PointF((point.x - dx) / drawScale, (point.y - dy) / drawScale)
                                            }
                                            activeStroke = emptyList()
                                            if (bitmapPoints.isNotEmpty()) {
                                                model.paintStroke(bitmapPoints, brushWidthBitmap)
                                            }
                                        },
                                        onDragCancel = { activeStroke = emptyList() }
                                    )
                                } else {
                                    detectTapGestures { tap ->
                                        touchPoint = tap
                                        touchFeedbackKey++
                                        val bx = ((tap.x - dx) / drawScale).toInt()
                                        val by = ((tap.y - dy) / drawScale).toInt()
                                        if (bx in 0 until bitmapWidth && by in 0 until bitmapHeight) {
                                            model.fillAt(bx, by)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        if (activeStroke.isNotEmpty()) {
                            Canvas(Modifier.fillMaxSize()) {
                                if (activeStroke.size == 1) {
                                    drawCircle(
                                        color = Color(ui.selectedColor),
                                        radius = brushWidthPx / 2f,
                                        center = activeStroke.first()
                                    )
                                } else {
                                    val path = Path().apply {
                                        moveTo(activeStroke.first().x, activeStroke.first().y)
                                        activeStroke.drop(1).forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(ui.selectedColor),
                                        style = Stroke(
                                            width = brushWidthPx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                        ColoringTouchSparkle(point = touchPoint, trigger = touchFeedbackKey)
                        if (ui.isBusy) {
                            Surface(
                                modifier = Modifier.align(Alignment.Center),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 6.dp
                            ) {
                                CircularProgressIndicator(Modifier.padding(13.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ColoringBoatCompanion() {
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "boat-companion")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boat-bob"
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Image(
            painter = painterResource(R.drawable.barco_icon_3d),
            contentDescription = "Barquito compañero",
            modifier = Modifier
                .size(92.dp)
                .graphicsLayer { translationY = bob * density.density }
                .semantics { contentDescription = "Barquito compañero animado" },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ColoringPaletteSwatch(
    color: PaletteColor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "palette-pop-${color.name}"
    )
    Surface(
        modifier = Modifier
            .size(60.dp, 70.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Color ${color.name}" },
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = if (selected) 8.dp else 3.dp,
        border = BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) Color(0xFFFFC857) else Color.White
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(38.dp, 55.dp)) {
                val pencilColor = Color(color.value)
                drawRoundRect(
                    color = pencilColor,
                    topLeft = Offset(size.width * 0.28f, size.height * 0.08f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.44f, size.height * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.32f),
                    topLeft = Offset(size.width * 0.35f, size.height * 0.12f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.07f, size.height * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.22f),
                    start = Offset(size.width * 0.28f, size.height * 0.66f),
                    end = Offset(size.width * 0.72f, size.height * 0.66f),
                    strokeWidth = 2.dp.toPx()
                )
                val tip = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.68f)
                    lineTo(size.width * 0.72f, size.height * 0.68f)
                    lineTo(size.width * 0.5f, size.height * 0.96f)
                    close()
                }
                drawPath(tip, pencilColor.copy(alpha = 0.82f))
                drawCircle(Color(0xFF4D453D), 2f, Offset(size.width * 0.5f, size.height * 0.92f))
            }
        }
    }
}

@Composable
private fun ColoringWidthButton(
    widthDp: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val widthPx = with(LocalDensity.current) { widthDp.dp.toPx() }
    val label = when (widthDp) {
        6 -> "fino"
        14 -> "medio"
        24 -> "grueso"
        else -> "muy grueso"
    }
    Surface(
        modifier = Modifier
            .size(68.dp, 60.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Grosor $label" },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFFFF1C7) else Color.White.copy(alpha = 0.92f),
        shadowElevation = if (selected) 7.dp else 3.dp,
        border = BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) Color(0xFFFFC857) else Color.White
        )
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 11.dp)) {
            drawLine(
                color = Color(0xFF246C70),
                start = Offset(2.dp.toPx(), size.height / 2f),
                end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                strokeWidth = widthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ColoringToolButton(
    icon: @Composable () -> Unit,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "tool-pop-$description"
    )
    Surface(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFFFFF1C7) else Color.White.copy(alpha = 0.92f),
        contentColor = Color(0xFF246C70),
        shadowElevation = if (selected) 8.dp else 4.dp,
        border = BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) Color(0xFFFFC857) else Color.White
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(32.dp)) { icon() }
        }
    }
}

@Composable
private fun ColoringTouchSparkle(point: Offset?, trigger: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = false
            delay(16)
            visible = true
            delay(430)
            visible = false
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "touch-sparkle"
    )
    if (point != null && progress > 0.01f) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = 15f + 23f * progress
            val alpha = (1f - progress) * 0.9f
            drawCircle(
                color = Color(0xFFFFC857).copy(alpha = alpha),
                radius = radius,
                center = point,
                style = Stroke(width = 4f)
            )
            val arm = radius * 0.72f
            drawLine(Color.White.copy(alpha = alpha), Offset(point.x - arm, point.y), Offset(point.x + arm, point.y), 3f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = alpha), Offset(point.x, point.y - arm), Offset(point.x, point.y + arm), 3f, StrokeCap.Round)
        }
    }
}
