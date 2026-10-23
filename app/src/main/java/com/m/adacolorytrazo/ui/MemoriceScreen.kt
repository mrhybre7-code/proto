package com.m.adacolorytrazo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m.adacolorytrazo.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class MemoryCard(
    val base: String,
    val pairId: Int,
    val faceResId: Int,
    val backResId: Int,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

private val memoryCardsSaver = listSaver<List<MemoryCard>, Any>(
    save = { cards ->
        cards.flatMap { card ->
            listOf(
                card.base,
                card.pairId,
                card.faceResId,
                card.backResId,
                card.isFaceUp,
                card.isMatched
            )
        }
    },
    restore = { saved ->
        val restored = saved.chunked(6).mapNotNull { fields ->
            val base = fields.getOrNull(0) as? String ?: return@mapNotNull null
            val pairId = (fields.getOrNull(1) as? Number)?.toInt()
                ?: return@mapNotNull null
            val faceResId = (fields.getOrNull(2) as? Number)?.toInt()
                ?: return@mapNotNull null
            val backResId = (fields.getOrNull(3) as? Number)?.toInt()
                ?: return@mapNotNull null
            val isFaceUp = fields.getOrNull(4) as? Boolean
                ?: return@mapNotNull null
            val isMatched = fields.getOrNull(5) as? Boolean
                ?: return@mapNotNull null

            MemoryCard(
                base = base,
                pairId = pairId,
                faceResId = faceResId,
                backResId = backResId,
                isFaceUp = isFaceUp,
                isMatched = isMatched
            )
        }
        // A coroutine cannot be restored. Hide a pending failed pair after rotation.
        if (restored.count { it.isFaceUp && !it.isMatched } >= 2) {
            restored.map { if (it.isMatched) it else it.copy(isFaceUp = false) }
        } else {
            restored
        }
    }
)

@Suppress("DiscouragedApi")
@Composable
fun MemoriceScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = rememberSound()
    val haptics = rememberHaptics()
    val speech = rememberSpeech()

    val pool = remember {
        listOf(
            "gato", "perro", "vaca", "oveja",
            "manzana", "platano", "uva", "frutilla",
            "auto", "avion", "tren", "bici"
        )
    }

    fun iconRes(base: String): Int {
        val threeD = ctx.resources.getIdentifier(
            "${base}_icon_3d", "drawable", ctx.packageName
        )
        if (threeD != 0) return threeD
        val r = ctx.resources.getIdentifier("${base}_icon", "drawable", ctx.packageName)
        return if (r != 0) r else R.drawable.ic_launcher_foreground
    }

    fun newGame(): List<MemoryCard> {
        val chosen = pool.shuffled().take(6)
        return chosen.flatMapIndexed { idx, base ->
            val face = iconRes(base)
            listOf(
                MemoryCard(
                    base = base,
                    pairId = idx,
                    faceResId = face,
                    backResId = R.drawable.ada_memorice_cardback
                ),
                MemoryCard(
                    base = base,
                    pairId = idx,
                    faceResId = face,
                    backResId = R.drawable.ada_memorice_cardback
                )
            )
        }.shuffled()
    }

    var cards by rememberSaveable(stateSaver = memoryCardsSaver) {
        mutableStateOf(newGame())
    }
    var first by rememberSaveable { mutableStateOf<Int?>(null) }
    // Transient lock is intentionally not saved across rotation/backgrounding.
    var lock by remember { mutableStateOf(false) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var generation by rememberSaveable { mutableIntStateOf(0) }
    val mismatchJob = remember { mutableStateOf<Job?>(null) }

    // Kept as semantic feedback for TalkBack; it is deliberately not rendered as a
    // reading exercise for toddlers. The board communicates state with stars, color,
    // card borders, haptics, sound, and the flip animation.
    var feedback by rememberSaveable { mutableStateOf("Busca dos dibujos iguales") }
    var showConfetti by rememberSaveable { mutableStateOf(false) }
    var burst by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(350)
        speech.speak("Busca dos dibujos iguales")
    }

    DisposableEffect(Unit) {
        onDispose {
            mismatchJob.value?.cancel()
            mismatchJob.value = null
        }
    }

    LaunchedEffect(burst) {
        if (burst > 0) {
            showConfetti = true
            delay(1200)
            showConfetti = false
        }
    }

    val matchedPairs = cards.count { it.isMatched } / 2
    val totalPairs = 6

    Box(modifier = modifier.fillMaxSize()) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            AdventureTopBar(
                title = "Memorice",
                subtitle = "Ada y sus amigos",
                onBack = onBack
            )

            MemoryProgress(
                matchedPairs = matchedPairs,
                totalPairs = totalPairs,
                moves = moves,
                feedback = feedback,
                modifier = Modifier.fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(cards) { idx, c ->
                    CuteMemoryCard(
                        baseName = c.base,
                        faceResId = c.faceResId,
                        isFaceUp = c.isFaceUp || c.isMatched,
                        isMatched = c.isMatched,
                        onClick = {
                            if (lock) return@CuteMemoryCard
                            if (c.isMatched) return@CuteMemoryCard
                            if (cards[idx].isFaceUp) return@CuteMemoryCard

                            cards = cards.toMutableList().also { list ->
                                list[idx] = list[idx].copy(isFaceUp = true)
                            }

                            if (first == null) {
                                first = idx
                            } else {
                                val a = first!!
                                first = null
                                moves++

                                val ca = cards[a]
                                val cb = cards[idx]

                                if (ca.pairId == cb.pairId) {
                                    val pairName = spokenName(
                                        category = memoryCategory(ca.base),
                                        id = ca.base
                                    )
                                    feedback = "Par encontrado: $pairName"
                                    sound.playCoin()
                                    haptics.reward()
                                    burst++

                                    cards = cards.toMutableList().also { list ->
                                        list[a] = list[a].copy(isMatched = true)
                                        list[idx] = list[idx].copy(isMatched = true)
                                    }

                                    if (cards.all { it.isMatched }) {
                                        feedback = "Todos los pares encontrados"
                                        speech.speak("¡Encontraste todos los pares! Dos iguales: $pairName.")
                                        burst++
                                    } else {
                                        speech.speak("¡Muy bien! Dos iguales: $pairName.")
                                    }
                                } else {
                                    feedback = "Las cartas se vuelven a ocultar"
                                    speech.speak("Casi. Vamos a buscar dos dibujos iguales.")
                                    lock = true

                                    mismatchJob.value?.cancel()
                                    val scheduledGeneration = generation
                                    val firstPairId = ca.pairId
                                    val secondPairId = cb.pairId
                                    mismatchJob.value = scope.launch {
                                        try {
                                            delay(1200)

                                            if (scheduledGeneration != generation) return@launch

                                            val currentCards = cards
                                            if (
                                                scheduledGeneration != generation ||
                                                a !in currentCards.indices ||
                                                idx !in currentCards.indices ||
                                                currentCards[a].pairId != firstPairId ||
                                                currentCards[idx].pairId != secondPairId
                                            ) {
                                                return@launch
                                            }

                                            cards = currentCards.toMutableList().also { list ->
                                                list[a] = list[a].copy(isFaceUp = false)
                                                list[idx] = list[idx].copy(isFaceUp = false)
                                            }
                                            if (scheduledGeneration == generation) {
                                                lock = false
                                            }
                                        } finally {
                                            if (scheduledGeneration == generation) {
                                                lock = false
                                                mismatchJob.value = null
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PlayfulActionPill(
                    text = "↻  Nuevo juego",
                    onClick = {
                        generation++
                        mismatchJob.value?.cancel()
                        mismatchJob.value = null
                        cards = newGame()
                        first = null
                        lock = false
                        moves = 0
                        feedback = "Busca dos dibujos iguales"
                        speech.speak("Vamos a buscar pares")
                    }
                )
            }
        }

        ConfettiOverlay(
            modifier = Modifier.fillMaxSize(),
            visible = showConfetti,
            burstKey = burst
        )
    }
}

@Composable
private fun MemoryProgress(
    matchedPairs: Int,
    totalPairs: Int,
    moves: Int,
    feedback: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .semantics {
                contentDescription = "$feedback. Progreso: $matchedPairs de $totalPairs pares. Movimientos: $moves."
            },
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFFCF3),
        shadowElevation = 9.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPairs) { index ->
                    ProgressStar(found = index < matchedPairs)
                }
            }
        }
    }
}

@Composable
private fun ProgressStar(found: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (found) 1f else 0.86f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "progress-star"
    )
    Surface(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = if (found) Color(0xFFFFD66D) else Color(0xFFE7F2EE),
        border = BorderStroke(1.dp, if (found) Color(0xFFF2B949) else Color(0xFFBBD7CA))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (found) {
                Text("★", color = Color(0xFF805B13), fontSize = 16.sp)
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9EC9B5))
                )
            }
        }
    }
}

@Composable
private fun MemoryCardBack() {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF31B8D5), Color(0xFF238FB6))))
            .border(2.dp, Color.White.copy(alpha = 0.9f), shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ada_memorice_cardback),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun CuteMemoryCard(
    baseName: String,
    faceResId: Int,
    isFaceUp: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 180f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "flip"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFaceUp || isMatched) 1.035f else 1f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "card-pop"
    )
    val idleTransition = rememberInfiniteTransition(label = "card-idle-$baseName")
    val idleBob by idleTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card-idle-bob-$baseName"
    )
    val category = memoryCategory(baseName)
    val label = spokenName(category = category, id = baseName)

    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = if (isMatched) 4.dp else 2.dp,
        shadowElevation = if (isFaceUp || isMatched) 14.dp else 8.dp,
        border = BorderStroke(
            if (isMatched) 3.dp else 1.dp,
            if (isMatched) Color(0xFFFFD75E) else Color.White.copy(alpha = 0.9f)
        ),
        modifier = Modifier
            .height(120.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 20f * density
                scaleX = scale
                scaleY = scale
                translationY = if (!isFaceUp && !isMatched) idleBob else 0f
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = !isMatched && !isFaceUp, role = Role.Button) { onClick() }
            .semantics {
                contentDescription = when {
                    isMatched -> "Par encontrado: $label"
                    isFaceUp -> "Figura descubierta: $label"
                    else -> "Carta oculta. Toca para descubrir el dibujo."
                }
                role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isMatched) Color(0xFFE8F5E9) else Color(0xFFFFFDF5)),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                MemoryCardBack()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = faceResId),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun memoryCategory(baseName: String): String = when (baseName) {
    "gato", "perro", "vaca", "oveja" -> "animales"
    "manzana", "platano", "uva", "frutilla" -> "frutas"
    else -> "transportes"
}
