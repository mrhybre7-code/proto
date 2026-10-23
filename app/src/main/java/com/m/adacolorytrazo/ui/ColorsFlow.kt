package com.m.adacolorytrazo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val COLOR_EXAMPLES: Map<String, String> = mapOf(
    "Rojo" to "Rojo como las frutillas, el camión de bomberos y el Viejito Pascuero (jo-jo-jo) ❤️",
    "Amarillo" to "Amarillo como el sol, los pollitos y el plátano 🌞",
    "Azul" to "Azul como el cielo, el mar y el pez cirujano azul 🌊",
    "Verde" to "Verde como el pasto, la lechuga y la manzana verde 🌿",
    "Naranjo" to "Naranjo como la mandarina, la zanahoria y una calabaza 🍊",
    "Morado" to "Morado como las uvas, las flores de lavanda y la betarraga 💜",
    "Rosado" to "Rosado como los flamencos, Peppa Pig y la pantera rosa 💗",
    "Café" to "Café como el chocolate, las hojas en otoño y la tierra húmeda 🤎",
    "Celeste" to "Celeste como el cielo clarito, una nube suave y un globo 🎈",
    "Gris" to "Gris como las nubes en un día de lluvia, una roca y un elefantito 🐘",
    "Negro" to "Negro como la noche, las arañas y los gorilas 🖤",
    "Blanco" to "Blanco como la nieve, la leche y el conejo de Pascua 🤍"
)

private data class UiColor(val name: String, val color: Color)

private val COLOR_LIST = listOf(
    UiColor("Rojo", Color(0xFFE53935)),
    UiColor("Amarillo", Color(0xFFFFEB3B)),
    UiColor("Azul", Color(0xFF1E88E5)),
    UiColor("Verde", Color(0xFF43A047)),
    UiColor("Naranjo", Color(0xFFFFA726)),
    UiColor("Morado", Color(0xFF8E24AA)),
    UiColor("Rosado", Color(0xFFF06292)),
    UiColor("Café", Color(0xFF8D6E63)),
    UiColor("Celeste", Color(0xFF80DEEA)),
    UiColor("Gris", Color(0xFFB0BEC5)),
    UiColor("Negro", Color(0xFF000000)),
    UiColor("Blanco", Color(0xFFFFFFFF))
)

@Composable
fun ColorsFlow(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    var target by remember { mutableStateOf(randomColorName(exclude = null)) }
    var feedback by remember { mutableStateOf("Busca el color indicado.") }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var answerAccepted by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }

    LaunchedEffect(burst) {
        if (burst > 0) {
            showConfetti = true
            delay(1200)
            showConfetti = false
        }
    }
    LaunchedEffect(answerAccepted) {
        if (answerAccepted) {
            delay(1200)
            val nextTarget = randomColorName(exclude = target)
            target = nextTarget
            feedback = "Encuentra el color $nextTarget."
            isCorrect = null
            answerAccepted = false
        }
    }

    val targetColor = COLOR_LIST.first { it.name == target }.color

    LaunchedEffect(target) {
        delay(350)
        speech.speak("Busca el color $target")
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdventureTopBar(
                title = "Colores",
                subtitle = "Mira, encuentra y aprende",
                onBack = onBack
            )
            Surface(
                modifier = Modifier.fillMaxWidth().height(88.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFFFFFCF3),
                shadowElevation = 11.dp,
                tonalElevation = 3.dp,
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.96f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .semantics { contentDescription = "Muestra del color que Ada dice en voz alta" },
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.64f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎨", fontSize = 37.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable(role = Role.Button) { speech.speak("Busca el color $target") }
                            .semantics {
                                contentDescription = "Repetir el nombre del color"
                                role = Role.Button
                            },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = targetColor,
                        shadowElevation = 7.dp,
                        border = BorderStroke(3.dp, Color.White.copy(alpha = 0.96f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔊", fontSize = 24.sp)
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f, fill = true)
            ) {
                COLOR_LIST.chunked(3).forEach { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowColors.forEach { uiColor ->
                            ColorCard(
                                uiColor = uiColor,
                                enabled = !answerAccepted,
                                onClick = {
                                    if (!answerAccepted) {
                                        if (uiColor.name == target) {
                                            feedback = "¡Excelente! Encontraste el color $target."
                                            isCorrect = true
                                            answerAccepted = true
                                            sound.playCoin()
                                            haptics.reward()
                                            speech.speak("¡Muy bien! Encontraste el color $target.")
                                            burst++
                                        } else {
                                            feedback = COLOR_EXAMPLES[uiColor.name]
                                                ?: "Ese es ${uiColor.name}; vuelve a buscar $target."
                                            isCorrect = false
                                            speech.speak("Este es ${uiColor.name}. Busquemos el color $target.")
                                        }
                                    }
                                }
                            )
                        }
                        if (rowColors.size < 3) {
                            repeat(3 - rowColors.size) {
                                Box(Modifier.weight(1f).height(70.dp))
                            }
                        }
                    }
                }
            }

            AdventureStatusCard(message = feedback, isCorrect = isCorrect, visualOnly = true)
        }
        ConfettiOverlay(
            modifier = Modifier.fillMaxSize(),
            visible = showConfetti,
            burstKey = burst
        )
    }
}

@Composable
private fun RowScope.ColorCard(
    uiColor: UiColor,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(25.dp)
    Surface(
        modifier = Modifier
            .weight(1f, fill = true)
            .height(76.dp)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Color ${uiColor.name}"
                role = Role.Button
            },
        shape = shape,
        color = Color.White.copy(alpha = 0.38f),
        shadowElevation = if (enabled) 8.dp else 2.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.78f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 5.dp)) {
                val crayonWidth = size.width * 0.34f
                val bodyHeight = size.height * 0.58f
                val tipHeight = size.height * 0.22f
                val left = (size.width - crayonWidth) / 2f
                val tipTop = size.height * 0.06f
                val bodyTop = tipTop + tipHeight * 0.82f
                val radius = androidx.compose.ui.geometry.CornerRadius(crayonWidth * 0.20f)
                val bodyRect = androidx.compose.ui.geometry.Offset(left, bodyTop)
                val bodySize = androidx.compose.ui.geometry.Size(crayonWidth, bodyHeight)
                val shade = uiColor.color.copy(alpha = 0.68f)
                val highlight = Color.White.copy(alpha = 0.45f)
                rotate(degrees = if (uiColor.name.hashCode() % 2 == 0) -7f else 7f) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.18f),
                        topLeft = androidx.compose.ui.geometry.Offset(bodyRect.x + 2.dp.toPx(), bodyRect.y + 3.dp.toPx()),
                        size = bodySize,
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(highlight, uiColor.color, shade)),
                        topLeft = bodyRect,
                        size = bodySize,
                        cornerRadius = radius
                    )
                    if (uiColor.color == Color.White) {
                        drawRoundRect(
                            color = Color(0xFF9BA8A7),
                            topLeft = bodyRect,
                            size = bodySize,
                            cornerRadius = radius,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    val tip = Path().apply {
                        moveTo(size.width / 2f, tipTop)
                        lineTo(left + crayonWidth, bodyTop + 2.dp.toPx())
                        lineTo(left, bodyTop + 2.dp.toPx())
                        close()
                    }
                    drawPath(tip, uiColor.color)
                    drawLine(
                        color = Color.White.copy(alpha = 0.68f),
                        start = androidx.compose.ui.geometry.Offset(left + crayonWidth * 0.23f, bodyTop + 7.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(left + crayonWidth * 0.23f, bodyTop + bodyHeight * 0.76f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.22f),
                        start = androidx.compose.ui.geometry.Offset(left + 1.dp.toPx(), bodyTop + bodyHeight * 0.78f),
                        end = androidx.compose.ui.geometry.Offset(left + crayonWidth - 1.dp.toPx(), bodyTop + bodyHeight * 0.78f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFF4C4036),
                        radius = 2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, tipTop + 2.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun randomColorName(exclude: String?): String {
    if (COLOR_LIST.size <= 1) return COLOR_LIST.first().name
    val choices = COLOR_LIST.map { it.name }.filterNot { it == exclude }
    return choices[Random.nextInt(choices.size)]
}
