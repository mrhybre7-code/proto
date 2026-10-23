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

private val TRANSPORT_DRAWABLES = mapOf(
    "auto" to R.drawable.auto_icon_3d,
    "avion" to R.drawable.avion_icon_3d,
    "bici" to R.drawable.bici_icon_3d,
    "barco" to R.drawable.barco_icon_3d,
    "ambulancia" to R.drawable.ambulancia_icon_3d,
    "policia" to R.drawable.policia_icon_3d,
    "tren" to R.drawable.tren_icon_3d,
    "moto" to R.drawable.moto_icon_3d
)

@Composable
fun RecognizeTransportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    val allTransports = remember { listOf("auto", "avion", "bici", "barco", "ambulancia", "policia", "tren", "moto") }

    var target by remember { mutableStateOf(allTransports.random()) }
    var feedback by remember { mutableStateOf("¡Vamos a jugar! Encuentra el transporte.") }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var answerAccepted by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(0) }

    val targetName = prettyName(category = "transportes", id = target)
    val spokenTargetName = spokenName(category = "transportes", id = target)
    val targetDrawable = TRANSPORT_DRAWABLES[target] ?: R.drawable.ic_launcher_foreground

    LaunchedEffect(target) {
        delay(350)
        speech.speak("Busca el transporte $spokenTargetName")
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
            delay(1200)
            val nextTarget = allTransports.filterNot { it == target }.random()
            target = nextTarget
            feedback = "Encuentra el transporte ${spokenName(category = "transportes", id = nextTarget)}."
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
                title = "Reconocer transportes",
                subtitle = "Descubre cómo nos movemos",
                onBack = onBack
            )
            AdventureTargetCard(
                eyebrow = "Encuentra",
                targetName = targetName,
                illustrationRes = targetDrawable,
                instruction = "Toca el vehículo correcto.",
                onRepeat = { speech.speak("Busca el transporte $spokenTargetName") },
                showInstruction = false,
                showTargetName = false
            )

            val options = remember(target) {
                allTransports.filterNot { it == target }.shuffled().take(3).plus(target).shuffled()
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = options, key = { it }) { transport ->
                    AdventureOptionTile(
                        title = prettyName(category = "transportes", id = transport),
                        description = spokenName(category = "transportes", id = transport),
                        illustrationRes = TRANSPORT_DRAWABLES[transport] ?: R.drawable.ic_launcher_foreground,
                        tint = PlayfulCardTints[allTransports.indexOf(transport).coerceAtLeast(0) % PlayfulCardTints.size],
                        enabled = !answerAccepted,
                        showTitle = false,
                        onClick = {
                            if (!answerAccepted && transport == target) {
                                val correctMessage = "¡Excelente! Encontraste el transporte $spokenTargetName."
                                feedback = correctMessage
                                isCorrect = true
                                answerAccepted = true
                                sound.playCoin()
                                haptics.reward()
                                speech.speak(correctMessage)
                                burst++
                            } else if (!answerAccepted) {
                                val wrongName = spokenName(category = "transportes", id = transport)
                                val errorMessage = "Ese es $wrongName. Busca el transporte $spokenTargetName."
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
