package com.m.adacolorytrazo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.m.adacolorytrazo.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class BodyPart(
    val id: String,
    val drawableName: String,
    val start: Offset = Offset.Zero, // compatibilidad; la posición inicial se calcula responsive
    val visualScale: Float = 1f // Sólo escala la imagen; conserva el área base para arrastrar.
)

private data class BodyLayout(
    val widthPx: Int,
    val heightPx: Int,
    val pieceSizePx: Int,
    val assemblyHeightPx: Int,
    val trayTopPx: Int,
    val trayHeightPx: Int,
    val dragBottomPx: Int,
    val columns: Int
)

private val bodyOffsetsSaver = listSaver<Map<String, Offset>, Any>(
    save = { offsets ->
        offsets.flatMap { (id, offset) -> listOf(id, offset.x, offset.y) }
    },
    restore = { saved ->
        saved.chunked(3).mapNotNull { fields ->
            val id = fields.getOrNull(0) as? String ?: return@mapNotNull null
            val x = (fields.getOrNull(1) as? Number)?.toFloat()
                ?: return@mapNotNull null
            val y = (fields.getOrNull(2) as? Number)?.toFloat()
                ?: return@mapNotNull null
            id to Offset(x, y)
        }.toMap()
    }
)

private fun clampBodyOffset(offset: Offset, layout: BodyLayout): Offset {
    val maxX = (layout.widthPx - layout.pieceSizePx).coerceAtLeast(0).toFloat()
    val maxY = (layout.dragBottomPx - layout.pieceSizePx).coerceAtLeast(0).toFloat()
    return Offset(
        offset.x.coerceIn(0f, maxX),
        offset.y.coerceIn(0f, maxY)
    )
}

private fun initialBodyOffsets(parts: List<BodyPart>, layout: BodyLayout): Map<String, Offset> {
    val gap = (layout.pieceSizePx * 0.10f).roundToInt().coerceAtLeast(4)
    return parts.mapIndexed { index, part ->
        val column = index % layout.columns
        val row = index / layout.columns
        val offset = Offset(
            x = (column * (layout.pieceSizePx + gap)).toFloat(),
            y = (layout.trayTopPx + row * (layout.pieceSizePx + gap)).toFloat()
        )
        part.id to clampBodyOffset(offset, layout)
    }.toMap()
}

@Suppress("DiscouragedApi")
@Composable
fun BodyAssembleScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current

    val density = LocalDensity.current

    // Las piezas comienzan en una bandeja calculada para el tamaño real de la ventana.
    val parts = remember {
        listOf(
            BodyPart("cara", "cara_icon", visualScale = 1.16f),
            BodyPart("cabello", "cabello_icon", visualScale = 1.10f),
            BodyPart("oreja_izq", "oreja_izquierda_icon"),
            BodyPart("oreja_der", "oreja_derecha_icon"),
            BodyPart("ojos", "ojos_icon", visualScale = 0.54f),
            BodyPart("nariz", "nariz_icon"),
            BodyPart("boca", "boca_icon", visualScale = 0.60f),
            BodyPart("torso", "torso_icon"),
            BodyPart("brazo_izq", "brazo_izquierdo_icon"),
            BodyPart("brazo_der", "brazo_derecho_icon"),
            BodyPart("pierna_izq", "pierna_izquierda_icon"),
            BodyPart("pierna_der", "pierna_derecha_icon")
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        val widthPx = with(density) { maxWidth.toPx() }.roundToInt().coerceAtLeast(1)
        val heightPx = with(density) { maxHeight.toPx() }.roundToInt().coerceAtLeast(1)
        val gapPx = with(density) { 8.dp.toPx() }.roundToInt().coerceAtLeast(4)
        val controlsHeightPx = with(density) { 80.dp.toPx() }.roundToInt()
        val dragBottomPx = (heightPx - controlsHeightPx - gapPx).coerceAtLeast(1)
        val playHeightPx = (dragBottomPx - gapPx).coerceAtLeast(1)
        val columns = when {
            maxWidth >= 520.dp -> 6
            maxWidth >= 300.dp -> 4
            else -> 3
        }
        val rows = (parts.size + columns - 1) / columns
        val trayGapPx = with(density) { 8.dp.toPx() }.roundToInt().coerceAtLeast(4)
        val desiredAssemblyPx = (playHeightPx * 0.42f).roundToInt()
            .coerceIn(
                with(density) { 120.dp.toPx() }.roundToInt(),
                with(density) { 340.dp.toPx() }.roundToInt()
            )
        val maxPieceByWidth = (
            (widthPx - trayGapPx * (columns + 1)) / columns
            ).coerceAtLeast(36)
        val maxPieceByHeight = (
            (playHeightPx - desiredAssemblyPx - gapPx - trayGapPx * (rows - 1)) / rows
            ).coerceAtLeast(36)
        val pieceSizePx = minOf(
            with(density) { 76.dp.toPx() }.roundToInt(),
            maxPieceByWidth,
            maxPieceByHeight
        ).coerceAtLeast(36)
        val trayHeightPx = rows * pieceSizePx + (rows - 1) * trayGapPx
        val assemblyHeightPx = desiredAssemblyPx
            .coerceAtMost((playHeightPx - trayHeightPx - gapPx).coerceAtLeast(0))
        val trayTopPx = assemblyHeightPx + gapPx
        val layout = BodyLayout(
            widthPx = widthPx,
            heightPx = heightPx,
            pieceSizePx = pieceSizePx,
            assemblyHeightPx = assemblyHeightPx,
            trayTopPx = trayTopPx,
            trayHeightPx = trayHeightPx,
            dragBottomPx = dragBottomPx,
            columns = columns
        )

        var offsets by rememberSaveable(stateSaver = bodyOffsetsSaver) {
            mutableStateOf(emptyMap<String, Offset>())
        }
        var showAssemblyHint by rememberSaveable { mutableStateOf(true) }
        var showDragSparkle by remember { mutableStateOf(false) }
        var previousWidthPx by rememberSaveable { mutableIntStateOf(0) }
        var previousHeightPx by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(showDragSparkle) {
            if (showDragSparkle) {
                delay(850L)
                showDragSparkle = false
            }
        }

        LaunchedEffect(layout) {
            val nextOffsets = if (offsets.isEmpty()) {
                initialBodyOffsets(parts, layout)
            } else if (previousWidthPx > 0 && previousHeightPx > 0) {
                offsets.mapValues { (_, offset) ->
                    clampBodyOffset(
                        Offset(
                            offset.x * widthPx / previousWidthPx.toFloat(),
                            offset.y * heightPx / previousHeightPx.toFloat()
                        ),
                        layout
                    )
                }
            } else {
                offsets.mapValues { (_, offset) -> clampBodyOffset(offset, layout) }
            }
            offsets = nextOffsets
            previousWidthPx = widthPx
            previousHeightPx = heightPx
        }

        // Zona superior sugerida; el armado sigue siendo libre y no tiene evaluación.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layout.assemblyHeightPx.toDp() }),
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.74f),
            shadowElevation = 5.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 30.dp, vertical = 5.dp)
                ) {
                    val scale = minOf(size.width / 190f, size.height / 250f)
                    val centerX = size.width / 2f
                    val top = (size.height - 250f * scale) / 2f
                    val guide = Color(0xFF79B9C5).copy(alpha = 0.27f)
                    val outline = Color.White.copy(alpha = 0.82f)
                    val headCenter = androidx.compose.ui.geometry.Offset(centerX, top + 35f * scale)
                    drawCircle(outline, 28f * scale, headCenter)
                    drawCircle(guide, 24f * scale, headCenter)
                    drawRoundRect(
                        color = outline,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - 12f * scale, top + 58f * scale),
                        size = androidx.compose.ui.geometry.Size(24f * scale, 24f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale)
                    )
                    drawRoundRect(
                        color = guide,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - 9f * scale, top + 60f * scale),
                        size = androidx.compose.ui.geometry.Size(18f * scale, 22f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f * scale)
                    )
                    drawRoundRect(
                        color = outline,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - 39f * scale, top + 75f * scale),
                        size = androidx.compose.ui.geometry.Size(78f * scale, 80f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f * scale)
                    )
                    drawRoundRect(
                        color = guide,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - 35f * scale, top + 79f * scale),
                        size = androidx.compose.ui.geometry.Size(70f * scale, 72f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * scale)
                    )
                    val limbWidth = 18f * scale
                    val legWidth = 22f * scale
                    val armPoints = listOf(
                        androidx.compose.ui.geometry.Offset(centerX - 32f * scale, top + 88f * scale) to androidx.compose.ui.geometry.Offset(centerX - 62f * scale, top + 126f * scale),
                        androidx.compose.ui.geometry.Offset(centerX - 62f * scale, top + 126f * scale) to androidx.compose.ui.geometry.Offset(centerX - 57f * scale, top + 158f * scale),
                        androidx.compose.ui.geometry.Offset(centerX + 32f * scale, top + 88f * scale) to androidx.compose.ui.geometry.Offset(centerX + 62f * scale, top + 126f * scale),
                        androidx.compose.ui.geometry.Offset(centerX + 62f * scale, top + 126f * scale) to androidx.compose.ui.geometry.Offset(centerX + 57f * scale, top + 158f * scale)
                    )
                    armPoints.forEach { (start, end) ->
                        drawLine(outline, start, end, limbWidth + 5f * scale, cap = StrokeCap.Round)
                        drawLine(guide, start, end, limbWidth, cap = StrokeCap.Round)
                    }
                    val legPoints = listOf(
                        androidx.compose.ui.geometry.Offset(centerX - 17f * scale, top + 145f * scale) to androidx.compose.ui.geometry.Offset(centerX - 22f * scale, top + 193f * scale),
                        androidx.compose.ui.geometry.Offset(centerX - 22f * scale, top + 193f * scale) to androidx.compose.ui.geometry.Offset(centerX - 25f * scale, top + 238f * scale),
                        androidx.compose.ui.geometry.Offset(centerX + 17f * scale, top + 145f * scale) to androidx.compose.ui.geometry.Offset(centerX + 22f * scale, top + 193f * scale),
                        androidx.compose.ui.geometry.Offset(centerX + 22f * scale, top + 193f * scale) to androidx.compose.ui.geometry.Offset(centerX + 25f * scale, top + 238f * scale)
                    )
                    legPoints.forEach { (start, end) ->
                        drawLine(outline, start, end, legWidth + 5f * scale, cap = StrokeCap.Round)
                        drawLine(guide, start, end, legWidth, cap = StrokeCap.Round)
                    }
                }
                AnimatedVisibility(
                    visible = showAssemblyHint,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text(
                        "Arma aquí el cuerpo 🧍\nArrastra cada pieza donde quieras",
                        color = Color(0xFF24495A)
                    )
                }
                AnimatedVisibility(
                    visible = !showAssemblyHint && showDragSparkle,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text("✨", fontSize = 34.sp)
                }
            }
        }

        // Bandeja responsive: su alto y columnas cambian con la ventana.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { layout.trayHeightPx.toDp() })
                .offset { IntOffset(0, layout.trayTopPx) },
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.64f),
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f))
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                Text(
                    "Piezas para arrastrar",
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0xFF527177)
                )
            }
        }

        // Partes arrastrables
        parts.forEach { part ->
            val resId = remember(part.drawableName) {
                val r = ctx.resources.getIdentifier(part.drawableName, "drawable", ctx.packageName)
                if (r != 0) r else R.drawable.ic_launcher_foreground
            }

            val off = offsets[part.id] ?: Offset.Zero

            Image(
                painter = painterResource(id = resId),
                contentDescription = part.id,
                modifier = Modifier
                    .offset { IntOffset(off.x.roundToInt(), off.y.roundToInt()) }
                    .size(with(density) { layout.pieceSizePx.toDp() })
                    .shadow(9.dp, RoundedCornerShape(20.dp), clip = false)
                    .graphicsLayer {
                        scaleX = part.visualScale
                        scaleY = part.visualScale
                    }
                    .pointerInput(part.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            showAssemblyHint = false
                            showDragSparkle = true
                            waitForUpOrCancellation()
                        }
                    }
                    .pointerInput(part.id, layout) {
                        detectDragGestures(
                            onDragStart = {
                                // El toque ya cerró la guía; el arrastre mantiene el feedback.
                                showAssemblyHint = false
                                showDragSparkle = true
                            },
                            onDrag = { _, dragAmount ->
                                offsets = offsets.toMutableMap().also { map ->
                                    val current = map[part.id] ?: Offset.Zero
                                    map[part.id] = clampBodyOffset(current + dragAmount, layout)
                                }
                            }
                        )
                    }
            )
        }

        // Botones abajo
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayfulActionPill(text = "🔄  Reiniciar piezas", onClick = {
                offsets = initialBodyOffsets(parts, layout)
                showAssemblyHint = true
                showDragSparkle = false
            })
        }
    }
}
