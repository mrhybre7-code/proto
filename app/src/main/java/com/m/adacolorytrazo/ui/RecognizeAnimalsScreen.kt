package com.m.adacolorytrazo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m.adacolorytrazo.R
import kotlinx.coroutines.delay

private val ANIMAL_DRAWABLES = mapOf(
    "gato" to R.drawable.gato_icon_3d,
    "pollito" to R.drawable.pollito_icon_3d,
    "caballo" to R.drawable.caballo_icon_3d,
    "pato" to R.drawable.pato_icon_3d,
    "cerdo" to R.drawable.cerdo_icon_3d,
    "perro" to R.drawable.perro_icon_3d,
    "gusano" to R.drawable.gusano_icon_3d,
    "vaca" to R.drawable.vaca_icon_3d,
    "oveja" to R.drawable.oveja_icon_3d,
    "conejo" to R.drawable.conejo_icon_3d
)

@Composable
fun RecognizeAnimalsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    val allAnimals = remember {
        listOf("gato", "pollito", "caballo", "pato", "cerdo", "perro", "gusano", "vaca", "oveja", "conejo")
    }

    var target by remember { mutableStateOf(allAnimals.random()) }
    var feedback by remember { mutableStateOf("¡Vamos a jugar! Escucha la consigna y encuentra la imagen.") }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var answerAccepted by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(0) }

    val targetName = prettyName(category = "animales", id = target)
    val spokenTargetName = spokenName(category = "animales", id = target)
    val targetDrawable = ANIMAL_DRAWABLES[target] ?: R.drawable.ic_launcher_foreground

    LaunchedEffect(target) {
        delay(350)
        speech.speak("Busca el animal $spokenTargetName")
    }

    LaunchedEffect(burst) {
        if (burst > 0) {
            showConfetti = true
            delay(1200)
            showConfetti = false
        }
    }
    LaunchedEffect(answerAccepted) {
        if (answerAccepted) {
            // Mantiene juntos el acierto, la voz y la consigna a la que corresponden.
            delay(1200)
            val nextTarget = allAnimals.filterNot { it == target }.random()
            target = nextTarget
            feedback = "Encuentra al animal ${spokenName(category = "animales", id = nextTarget)}."
            isCorrect = null
            answerAccepted = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AdventureTopBar(
                title = "Reconocer animales",
                subtitle = "Una aventura para aprender",
                onBack = onBack
            )
            AdventureTargetCard(
                eyebrow = "Encuentra a",
                targetName = targetName,
                illustrationRes = targetDrawable,
                instruction = "Toca el animal que escuchas y ves.",
                onRepeat = { speech.speak("Busca el animal $spokenTargetName") },
                showInstruction = false,
                showTargetName = false
            )

            val options = remember(target) {
                allAnimals.filterNot { it == target }.shuffled().take(3).plus(target).shuffled()
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = options, key = { it }) { animal ->
                    AdventureOptionTile(
                        title = prettyName(category = "animales", id = animal),
                        description = spokenName(category = "animales", id = animal),
                        illustrationRes = ANIMAL_DRAWABLES[animal] ?: R.drawable.ic_launcher_foreground,
                        tint = PlayfulCardTints[ANIMAL_DRAWABLES.keys.indexOf(animal).coerceAtLeast(0) % PlayfulCardTints.size],
                        enabled = !answerAccepted,
                        showTitle = false,
                        onClick = {
                            if (!answerAccepted && animal == target) {
                                val correctMessage = "¡Muy bien! Encontraste el animal $spokenTargetName."
                                feedback = correctMessage
                                isCorrect = true
                                answerAccepted = true
                                sound.playCoin()
                                haptics.reward()
                                speech.speak(correctMessage)
                                burst++
                            } else if (!answerAccepted) {
                                val wrongName = spokenName(category = "animales", id = animal)
                                val errorMessage = "Ese es $wrongName. Busca el animal $spokenTargetName."
                                feedback = errorMessage
                                isCorrect = false
                                speech.speak(errorMessage)
                            }
                        }
                    )
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
