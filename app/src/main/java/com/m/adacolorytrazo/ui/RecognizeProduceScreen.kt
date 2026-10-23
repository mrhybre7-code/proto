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

private val PRODUCE_DRAWABLES = mapOf(
    "manzana" to R.drawable.manzana_icon_3d,
    "platano" to R.drawable.platano_icon_3d,
    "frutilla" to R.drawable.frutilla_icon_3d,
    "pera" to R.drawable.pera_icon_3d,
    "uva" to R.drawable.uva_icon_3d,
    "naranja" to R.drawable.naranja_icon_3d,
    "sandia" to R.drawable.sandia_icon_3d,
    "pina" to R.drawable.pina_icon_3d,
    "cereza" to R.drawable.cereza_icon_3d,
    "durazno" to R.drawable.durazno_icon_3d,
    "zanahoria" to R.drawable.zanahoria_icon_3d,
    "tomate" to R.drawable.tomate_icon_3d,
    "brocoli" to R.drawable.brocoli_icon_3d,
    "pepino" to R.drawable.pepino_icon_3d,
    "papa" to R.drawable.papa_icon_3d
)

@Composable
fun RecognizeProduceScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()
    val fruits = remember { listOf("manzana", "platano", "frutilla", "pera", "uva", "naranja", "sandia", "pina", "cereza", "durazno") }
    val vegetables = remember { listOf("zanahoria", "tomate", "brocoli", "pepino", "papa") }
    val allProduce = remember { fruits + vegetables }

    var target by remember { mutableStateOf(allProduce.random()) }
    var feedback by remember { mutableStateOf("¡Vamos a jugar! Encuentra la fruta o verdura.") }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var answerAccepted by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(0) }

    val targetCategory = if (target in fruits) "fruta" else "verdura"
    val targetGroup = if (target in fruits) "frutas" else "verduras"
    val targetPrettyName = prettyName(category = targetGroup, id = target)
    val spokenTargetName = spokenName(category = targetGroup, id = target)
    val targetDrawable = PRODUCE_DRAWABLES[target] ?: R.drawable.ic_launcher_foreground

    LaunchedEffect(target) {
        delay(350)
        speech.speak("Busca $spokenTargetName")
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
            val nextTarget = allProduce.filterNot { it == target }.random()
            target = nextTarget
            val nextGroup = if (nextTarget in fruits) "frutas" else "verduras"
            feedback = "Encuentra ${spokenName(category = nextGroup, id = nextTarget)}."
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
                title = "Frutas y verduras",
                subtitle = "Colores, formas y sabores",
                onBack = onBack
            )
            AdventureTargetCard(
                eyebrow = "Encuentra",
                targetName = targetPrettyName,
                illustrationRes = targetDrawable,
                instruction = "¿Cuál de estas imágenes es igual?",
                onRepeat = { speech.speak("Busca $spokenTargetName") },
                showInstruction = false,
                showTargetName = false
            )

            val options = remember(target) {
                allProduce.filterNot { it == target }.shuffled().take(3).plus(target).shuffled()
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = options, key = { it }) { produce ->
                    val group = if (produce in fruits) "frutas" else "verduras"
                    val optionCategory = if (produce in fruits) "fruta" else "verdura"
                    AdventureOptionTile(
                        title = prettyName(category = group, id = produce),
                        illustrationRes = PRODUCE_DRAWABLES[produce] ?: R.drawable.ic_launcher_foreground,
                        tint = PlayfulCardTints[allProduce.indexOf(produce).coerceAtLeast(0) % PlayfulCardTints.size],
                        enabled = !answerAccepted,
                        description = "${spokenName(category = group, id = produce)}, $optionCategory",
                        showTitle = false,
                        onClick = {
                            if (!answerAccepted && produce == target) {
                                val correctMessage = "¡Muy bien! $spokenTargetName es una $targetCategory."
                                feedback = correctMessage
                                isCorrect = true
                                answerAccepted = true
                                sound.playCoin()
                                haptics.reward()
                                speech.speak(correctMessage)
                                burst++
                            } else if (!answerAccepted) {
                                val wrongName = spokenName(category = group, id = produce)
                                val errorMessage = "Ese es $wrongName. Es una $optionCategory. Busca $spokenTargetName."
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
