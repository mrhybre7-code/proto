package com.m.adacolorytrazo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m.adacolorytrazo.R
import com.m.adacolorytrazo.coloring.ColoringScreen
import kotlinx.coroutines.delay

private data class Animal(
    val characterName: String,
    val speciesName: String,
    val resourceBase: String,
    val iconResId: Int,
    val emoji: String
)

// Correspondencia asumida: los nombres propios conservan sus ilustraciones existentes.
private val ANIMALS = listOf(
    Animal("Tobías", "perro", "perro", R.drawable.perro_icon_3d, "🐶"),
    Animal("Confeti", "gato", "gato", R.drawable.gato_icon_3d, "🐱"),
    Animal("Wormi", "gusano", "gusano", R.drawable.gusano_icon_3d, "🪱"),
    Animal("Cuco", "pollito", "pollito", R.drawable.pollito_icon_3d, "🐥"),
    Animal("Flica", "caballo", "caballo", R.drawable.caballo_icon_3d, "🐴"),
    Animal("Gingi", "pato", "pato", R.drawable.pato_icon_3d, "🦆"),
    Animal("Spunky", "cerdo", "cerdo", R.drawable.cerdo_icon_3d, "🐷")
)

@Composable
fun AnimalsFlow(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    var pickedBase by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = ANIMALS.firstOrNull { it.resourceBase == pickedBase }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }

    LaunchedEffect(burst) {
        if (burst > 0) {
            showConfetti = true
            delay(1300)
            showConfetti = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdventureTopBar(
                title = if (picked == null) "Animales" else "Colorea a ${picked.characterName}",
                subtitle = if (picked == null) "Elige un amigo para empezar" else "Dale vida con tus colores",
                onBack = onBack
            )

            if (picked == null) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(124.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    items(ANIMALS, key = { it.resourceBase }) { animal ->
                        val tint = PlayfulCardTints[ANIMALS.indexOf(animal) % PlayfulCardTints.size]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable {
                                    pickedBase = animal.resourceBase
                                    speech.speak("${animal.characterName}, un ${animal.speciesName}.")
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
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(104.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Brush.verticalGradient(listOf(tint, Color.White.copy(alpha = 0.6f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(animal.iconResId),
                                        contentDescription = "${animal.characterName}, ${animal.speciesName}",
                                        modifier = Modifier.size(98.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    text = animal.characterName,
                                    color = Color(0xFF24495A),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${animal.speciesName} ${animal.emoji}",
                                    color = Color(0xFF527177),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    ColoringScreen(
                        baseName = picked.resourceBase,
                        title = "¡A colorear ${picked.characterName}!"
                    )
                    ConfettiOverlay(
                        modifier = Modifier.fillMaxSize(),
                        visible = showConfetti,
                        burstKey = burst
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayfulActionPill(text = "Cambiar dibujo", onClick = { pickedBase = null }, accent = Color.White)
                    PlayfulActionPill(
                        text = "¡Terminé! 🎉",
                        onClick = {
                            sound.playCoin()
                            haptics.reward()
                            speech.speak("¡Muy bien! Terminaste de colorear a ${picked.characterName}.")
                            burst++
                        }
                    )
                }
            }
        }
    }
}
