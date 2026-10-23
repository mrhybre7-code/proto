package com.m.adacolorytrazo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.m.adacolorytrazo.R
import com.m.adacolorytrazo.util.ImageSaveResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class FreeDrawColor(val value: Color, val name: String)

private val FREE_DRAW_PALETTE = listOf(
    FreeDrawColor(Color(0xFF20242B), "negro"),
    FreeDrawColor(Color(0xFFFF5252), "rojo"),
    FreeDrawColor(Color(0xFFFFC107), "amarillo"),
    FreeDrawColor(Color(0xFF4CAF50), "verde"),
    FreeDrawColor(Color(0xFF03A9F4), "celeste"),
    FreeDrawColor(Color(0xFF7C4DFF), "violeta"),
    FreeDrawColor(Color(0xFFFF8A80), "salmón"),
    FreeDrawColor(Color(0xFFFFF176), "amarillo claro"),
    FreeDrawColor(Color(0xFFB9FBC0), "menta"),
    FreeDrawColor(Color(0xFFBDE0FE), "azul claro")
)

private val THICKNESSES = listOf(10f, 22f, 36f, 52f)

@Composable
fun FreeDrawScreen(
    onBack: () -> Unit = {},
    canvasHeight: Dp = 420.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val model: FreeDrawingViewModel = viewModel()

    fun performSave() {
        scope.launch {
            try {
                when (val result = model.save(
                    context.applicationContext,
                    "Ada_Dibujo_${System.currentTimeMillis()}.png"
                )) {
                    ImageSaveResult.Success -> snackbarHost.showSnackbar("Dibujo guardado en la galería")
                    is ImageSaveResult.Failure -> snackbarHost.showSnackbar("No se pudo guardar: ${result.reason}")
                }
            } catch (_: CancellationException) {
                // La cancelación de la pantalla limpia cualquier entrada pendiente.
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
        if (model.isSaving) return
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

    val adaMotion = rememberInfiniteTransition(label = "ada-free-draw")
    val adaBob by adaMotion.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ada-bob"
    )

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.ada_menu_educational_park),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHost) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FreeDrawBackButton(onClick = onBack)
                    FreeDrawSaveButton(
                        enabled = !model.isSaving,
                        saving = model.isSaving,
                        onClick = ::requestSave
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = canvasHeight.coerceAtMost(340.dp))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        shape = RoundedCornerShape(38.dp),
                        color = Color(0xFFFFFCF1),
                        shadowElevation = 14.dp,
                        border = BorderStroke(6.dp, Color(0xFFFFC24D))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .onSizeChanged(model::resize)
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .pointerInput(model.eraser, model.currentColor, model.currentWidth, model.isSaving) {
                                        detectDragGestures(
                                            onDragStart = model::beginStroke,
                                            onDrag = { change, _ -> model.appendPoint(change.position) },
                                            onDragEnd = model::endStroke,
                                            onDragCancel = model::cancelStroke
                                        )
                                    }
                            ) {
                                model.strokes.forEach { stroke ->
                                    val paintStyle = Stroke(
                                        width = stroke.width,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                    val path = if (stroke.points.size >= 2) {
                                        Path().apply {
                                            moveTo(stroke.points.first().x, stroke.points.first().y)
                                            for (point in stroke.points.drop(1)) lineTo(point.x, point.y)
                                        }
                                    } else {
                                        null
                                    }
                                    if (path != null) {
                                        drawPath(
                                            path = path,
                                            color = if (stroke.isEraser) Color.Transparent else stroke.color,
                                            style = paintStyle,
                                            blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
                                        )
                                    } else if (stroke.points.size == 1) {
                                        val point = stroke.points.first()
                                        drawCircle(
                                            color = if (stroke.isEraser) Color.Transparent else stroke.color,
                                            radius = stroke.width / 2f,
                                            center = point,
                                            blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
                                        )
                                    }
                                }
                            }
                            if (model.isSaving) {
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
                    }

                    Image(
                        painter = painterResource(R.drawable.ada_free_draw_crayon),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(width = 94.dp, height = 172.dp)
                            .graphicsLayer { translationY = adaBob }
                    )
                }

                FreeDrawSectionCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(FREE_DRAW_PALETTE, key = { it.name }) { color ->
                                FreeDrawCrayon(
                                    color = color,
                                    selected = color.value == model.currentColor && !model.eraser,
                                    enabled = !model.isSaving,
                                    onClick = { model.selectColor(color.value) }
                                )
                            }
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(THICKNESSES, key = { it }) { width ->
                                FreeDrawWidthButton(
                                    width = width,
                                    selected = model.currentWidth == width,
                                    enabled = !model.isSaving,
                                    onClick = { model.setWidth(width) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FreeDrawEraserButton(
                                selected = model.eraser,
                                enabled = !model.isSaving,
                                onClick = model::toggleEraser
                            )
                            Spacer(Modifier.size(7.dp))
                            FreeDrawActionButton(
                                icon = { Icon(Icons.Filled.Undo, contentDescription = null) },
                                description = "Deshacer",
                                containerColor = Color(0xFF8B4FE0),
                                enabled = !model.isSaving && model.canUndo,
                                onClick = model::undo
                            )
                            Spacer(Modifier.size(7.dp))
                            FreeDrawActionButton(
                                icon = { Icon(Icons.Filled.Redo, contentDescription = null) },
                                description = "Rehacer",
                                containerColor = Color(0xFF279CEB),
                                enabled = !model.isSaving && model.canRedo,
                                onClick = model::redo
                            )
                            Spacer(Modifier.size(7.dp))
                            FreeDrawActionButton(
                                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                description = "Borrar todo",
                                containerColor = Color(0xFFE94F63),
                                enabled = !model.isSaving && model.canUndo,
                                onClick = model::clear
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeDrawSectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFD69A58),
        shadowElevation = 10.dp,
        border = BorderStroke(3.dp, Color(0xFFFFE4B0))
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
private fun FreeDrawBackButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(54.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Volver" },
        shape = CircleShape,
        color = Color(0xFF3186E8),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun FreeDrawCrayon(
    color: FreeDrawColor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Color ${color.name}" },
        shape = CircleShape,
        color = if (selected) Color.White.copy(alpha = 0.82f) else Color.Transparent,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) BorderStroke(2.dp, Color(0xFFFFE082)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(36.dp, 52.dp)) {
                val body = Color(0xFF20242B)
                drawRoundRect(
                    color = color.value,
                    topLeft = Offset(size.width * 0.27f, size.height * 0.12f),
                    size = Size(size.width * 0.46f, size.height * 0.60f),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(size.width * 0.33f, size.height * 0.17f),
                    size = Size(size.width * 0.08f, size.height * 0.46f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawLine(
                    color = body.copy(alpha = 0.28f),
                    start = Offset(size.width * 0.27f, size.height * 0.69f),
                    end = Offset(size.width * 0.73f, size.height * 0.69f),
                    strokeWidth = 2f
                )
                val tip = Path().apply {
                    moveTo(size.width * 0.27f, size.height * 0.71f)
                    lineTo(size.width * 0.73f, size.height * 0.71f)
                    lineTo(size.width * 0.50f, size.height * 0.96f)
                    close()
                }
                drawPath(tip, color.value.copy(alpha = 0.78f))
                drawCircle(body.copy(alpha = 0.55f), 2.5f, Offset(size.width * 0.50f, size.height * 0.90f))
            }
        }
    }
}

@Composable
private fun FreeDrawWidthButton(
    width: Float,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val label = when {
        width <= 10f -> "fino"
        width <= 22f -> "medio"
        width <= 36f -> "grueso"
        else -> "muy grueso"
    }
    Surface(
        modifier = Modifier
            .size(58.dp, 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Grosor $label" },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFFFE9A8) else Color(0xFFFFF6E5),
        shadowElevation = if (selected) 5.dp else 2.dp,
        border = if (selected) BorderStroke(2.dp, Color(0xFFFFC857)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                drawLine(
                    color = Color(0xFF246C70),
                    start = Offset(2.dp.toPx(), size.height / 2f),
                    end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                    strokeWidth = width * 1.55f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun FreeDrawEraserButton(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Goma grande" },
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color(0xFFFFDBE7) else Color(0xFFFFF1F5),
        shadowElevation = if (selected) 6.dp else 2.dp,
        border = if (selected) BorderStroke(2.dp, Color(0xFFFF78A8)) else null
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            rotate(-35f) {
                drawRoundRect(
                    color = Color(0xFFFF8FB3),
                    topLeft = Offset(size.width * 0.20f, size.height * 0.25f),
                    size = Size(size.width * 0.60f, size.height * 0.50f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(size.width * 0.49f, size.height * 0.27f),
                    end = Offset(size.width * 0.49f, size.height * 0.73f),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun FreeDrawActionButton(
    icon: @Composable () -> Unit,
    description: String,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.45f),
        contentColor = Color.White,
        shadowElevation = if (enabled) 6.dp else 1.dp,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Box(Modifier.size(25.dp)) { icon() }
        }
    }
}

@Composable
private fun FreeDrawSaveButton(enabled: Boolean, saving: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Guardar dibujo" },
        shape = CircleShape,
        color = Color(0xFF38C871),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (saving) CircularProgressIndicator(Modifier.size(27.dp))
            else Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(30.dp))
        }
    }
}
