package com.m.adacolorytrazo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m.adacolorytrazo.R
import com.m.adacolorytrazo.coloring.ColoringScreen
import kotlinx.coroutines.delay

private data class DrawingOption(val base: String, val label: String)

private val FRUIT_DRAWINGS = listOf(
    DrawingOption("manzana", "Manzana"),
    DrawingOption("platano", "Plátano"),
    DrawingOption("uva", "Uva"),
    DrawingOption("frutilla", "Frutilla"),
    DrawingOption("pera", "Pera"),
    DrawingOption("sandia", "Sandía"),
    DrawingOption("pina", "Piña"),
    DrawingOption("naranja", "Naranja"),
    DrawingOption("cereza", "Cereza"),
    DrawingOption("durazno", "Durazno")
)

private val TRANSPORT_DRAWINGS = listOf(
    DrawingOption("bici", "Bicicleta"),
    DrawingOption("ambulancia", "Ambulancia"),
    DrawingOption("auto", "Auto"),
    DrawingOption("avion", "Avión"),
    DrawingOption("barco", "Barco"),
    DrawingOption("policia", "Policía"),
    DrawingOption("tren", "Tren")
)

@Composable
fun FruitsFlow(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    DrawingCategoryFlow("Frutas", FRUIT_DRAWINGS, modifier, onBack)
}

@Composable
fun TransportFlow(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    DrawingCategoryFlow("Transportes", TRANSPORT_DRAWINGS, modifier, onBack)
}

@Suppress("DiscouragedApi")
@Composable
private fun DrawingCategoryFlow(
    category: String,
    drawings: List<DrawingOption>,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    var pickedBase by rememberSaveable(category) { mutableStateOf<String?>(null) }
    val picked = drawings.firstOrNull { it.base == pickedBase }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }

    LaunchedEffect(burst) {
        if (burst > 0) {
            showConfetti = true
            delay(1300)
            showConfetti = false
        }
    }
    LaunchedEffect(pickedBase) {
        if (pickedBase == null) {
            delay(350)
            speech.speak("Elige un dibujo para colorear")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdventureTopBar(
                title = if (picked == null) category else "Colorea: ${picked.label}",
                subtitle = if (picked == null) "Escoge un dibujo para empezar" else "Pinta, rellena y guarda tu creación",
                onBack = onBack
            )
            if (picked == null) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(124.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    items(drawings, key = { it.base }) { drawing ->
                        val iconId = remember(drawing.base) {
                            val threeDId = context.resources.getIdentifier(
                                "${drawing.base}_icon_3d", "drawable", context.packageName
                            )
                            if (threeDId != 0) threeDId else context.resources.getIdentifier(
                                "${drawing.base}_icon", "drawable", context.packageName
                            )
                        }
                        val safeIconId = if (iconId != 0) iconId else R.drawable.ic_launcher_foreground
                        val tint = PlayfulCardTints[drawings.indexOf(drawing) % PlayfulCardTints.size]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable {
                                    pickedBase = drawing.base
                                    speech.speak("Vamos a colorear ${drawing.label}")
                                    sound.playCoin()
                                },
                            shape = RoundedCornerShape(26.dp),
                            color = Color.White,
                            shadowElevation = 9.dp,
                            tonalElevation = 2.dp,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.96f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(102.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Brush.verticalGradient(listOf(tint, Color.White.copy(alpha = 0.6f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(safeIconId),
                                        contentDescription = drawing.label,
                                        modifier = Modifier.size(95.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    text = drawing.label,
                                    color = Color(0xFF24495A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    ColoringScreen(
                        baseName = picked.base,
                        title = "¡A colorear ${picked.label}!"
                    )
                    ConfettiOverlay(
                        modifier = Modifier.fillMaxSize(),
                        visible = showConfetti,
                        burstKey = burst
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PlayfulActionPill(text = "Cambiar dibujo", onClick = { pickedBase = null }, accent = Color.White)
                    }
                    item {
                        PlayfulActionPill(
                            text = "¡Terminé! 🎉",
                            onClick = {
                                sound.playCoin()
                                haptics.reward()
                                burst++
                            }
                        )
                    }
                }
            }
        }
    }
}
