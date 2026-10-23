package com.m.adacolorytrazo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m.adacolorytrazo.R

private val AdaRounded = FontFamily(
    Font(R.font.fredoka_bold, weight = FontWeight.Bold),
    Font(R.font.fredoka_semibold, weight = FontWeight.SemiBold)
)

private enum class MenuVisual {
    Image,
    Crayons,
    ColorDots
}

private data class MenuActivity(
    val title: String,
    val imageRes: Int,
    val tint: Color,
    val onClick: () -> Unit,
    val isPortraitCard: Boolean = false,
    val visual: MenuVisual = MenuVisual.Image
)

private data class MenuCategory(
    val key: String,
    val title: String,
    val hint: String,
    val imageRes: Int,
    val secondaryImageRes: Int?,
    val colors: List<Color>,
    val activities: List<MenuActivity>,
    val isAdaArt: Boolean = false
)

@Composable
fun HomeMenuScreen(
    modifier: Modifier = Modifier,
    onAnimals: () -> Unit,
    onFruits: () -> Unit,
    onTransport: () -> Unit,
    onColors: () -> Unit,
    onFreeDraw: () -> Unit,
    onRecognizeAnimals: () -> Unit,
    onRecognizeFruits: () -> Unit,
    onRecognizeTransport: () -> Unit,
    onBodyAssemble: () -> Unit,
    onWeatherGame: () -> Unit,
    onMemorice: () -> Unit
) {
    val createActivities = listOf(
        MenuActivity("Animales", R.drawable.perro_icon_3d, Color(0xFFFFD3BA), onAnimals),
        MenuActivity("Frutas", R.drawable.manzana_icon_3d, Color(0xFFFFE08A), onFruits),
        MenuActivity("Transportes", R.drawable.auto_icon_3d, Color(0xFFBFE8FF), onTransport),
        MenuActivity(
            "Dibujo libre", 0, Color(0xFFD9C8FF), onFreeDraw,
            visual = MenuVisual.Crayons
        ),
        MenuActivity(
            "Colores", 0, Color(0xFFFFC4DF), onColors,
            visual = MenuVisual.ColorDots
        )
    )
    val discoverActivities = listOf(
        MenuActivity("Reconocer animales", R.drawable.gato_icon_3d, Color(0xFFC6EFA9), onRecognizeAnimals),
        MenuActivity("Frutas y verduras", R.drawable.zanahoria_icon_3d, Color(0xFFFFD19D), onRecognizeFruits),
        MenuActivity("Reconocer transportes", R.drawable.ambulancia_icon_3d, Color(0xFFBFE7FF), onRecognizeTransport)
    )
    val adaActivities = listOf(
        MenuActivity("Armar el cuerpo", R.drawable.cara_icon, Color(0xFFD8C8FF), onBodyAssemble),
        MenuActivity(
            "Memorice", R.drawable.ada_memorice_cardback, Color(0xFFBCEED9), onMemorice,
            isPortraitCard = true
        ),
        MenuActivity(
            "Vestir según el clima", R.drawable.ada_menu_wave, Color(0xFFFFD3E2), onWeatherGame,
            isPortraitCard = true
        )
    )

    val categories = listOf(
        MenuCategory(
            key = "create",
            title = "Crear",
            hint = "Dibuja y pinta",
            imageRes = R.drawable.manzana_icon_3d,
            secondaryImageRes = R.drawable.auto_icon_3d,
            colors = listOf(Color(0xFFFFB53E), Color(0xFFFFD65B)),
            activities = createActivities
        ),
        MenuCategory(
            key = "discover",
            title = "Descubrir",
            hint = "Animales, frutas y vehículos",
            imageRes = R.drawable.gato_icon_3d,
            secondaryImageRes = R.drawable.zanahoria_icon_3d,
            colors = listOf(Color(0xFF31C6A6), Color(0xFF87D96E)),
            activities = discoverActivities
        ),
        MenuCategory(
            key = "ada",
            title = "Jugar con Ada",
            hint = "Ada te invita a jugar",
            imageRes = R.drawable.ada_menu_wave,
            secondaryImageRes = null,
            colors = listOf(Color(0xFFAC70EC), Color(0xFFF17FB9)),
            activities = adaActivities,
            isAdaArt = true
        )
    )

    var selectedCategoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = categories.firstOrNull { it.key == selectedCategoryKey }
    BackHandler(enabled = selectedCategory != null) {
        selectedCategoryKey = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF72D3F7), Color(0xFFC9F1FF), Color(0xFFEAF8D4), Color(0xFFFFE9B9))
                )
            )
    ) {
        PlayfulGameBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedCategory == null) {
                MenuHero()
                categories.forEachIndexed { index, category ->
                    CategoryDoor(
                        category = category,
                        index = index,
                        onClick = { selectedCategoryKey = category.key }
                    )
                }
                Spacer(Modifier.height(10.dp))
            } else {
                CategoryHeader(
                    category = selectedCategory,
                    onBack = { selectedCategoryKey = null }
                )
                selectedCategory.activities.chunked(2).forEach { rowActivities ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (rowActivities.size == 1) {
                            Spacer(Modifier.weight(0.5f))
                            ActivityCard(rowActivities.first(), Modifier.weight(1f))
                            Spacer(Modifier.weight(0.5f))
                        } else {
                            rowActivities.forEach { activity ->
                                ActivityCard(activity, Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MenuHero() {
    val transition = rememberInfiniteTransition(label = "ada-menu-hello")
    val bob by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ada-menu-bob"
    )
    val sway by transition.animateFloat(
        initialValue = -0.7f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ada-menu-sway"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .padding(start = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Ada Color y Trazo" },
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "Ada",
                color = Color(0xFFE52C91),
                fontSize = 39.sp,
                lineHeight = 42.sp,
                fontFamily = AdaRounded,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Color y Trazo",
                color = Color(0xFF174E82),
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontFamily = AdaRounded,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Image(
            painter = painterResource(R.drawable.ada_menu_wave),
            contentDescription = "Ada de pie saludando",
            modifier = Modifier
                .width(132.dp)
                .height(172.dp)
                .graphicsLayer {
                    translationY = bob
                    rotationZ = sway
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun CategoryDoor(
    category: MenuCategory,
    index: Int,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "category-${category.key}")
    val bob by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800 + index * 170, delayMillis = index * 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "category-bob-${category.key}"
    )
    val shape = RoundedCornerShape(32.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .graphicsLayer { translationY = bob }
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Abrir ${category.title}. ${category.hint}"
                role = Role.Button
            },
        shape = shape,
        color = category.colors.first(),
        shadowElevation = 14.dp,
        tonalElevation = 3.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.88f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(category.colors))
        ) {
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 17.dp, y = 25.dp)
                    .background(Color.White.copy(alpha = 0.13f), CircleShape)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp, end = 116.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = category.title,
                    color = Color.White,
                    fontSize = if (category.title == "Jugar con Ada") 23.sp else 27.sp,
                    lineHeight = 30.sp,
                    fontFamily = AdaRounded,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = category.hint,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            CategoryArt(
                category = category,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 9.dp)
            )
        }
    }
}

@Composable
private fun CategoryArt(category: MenuCategory, modifier: Modifier = Modifier) {
    if (category.isAdaArt) {
        Image(
            painter = painterResource(category.imageRes),
            contentDescription = null,
            modifier = modifier
                .width(84.dp)
                .height(112.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
            category.secondaryImageRes?.let { secondary ->
                Image(
                    painter = painterResource(secondary),
                    contentDescription = null,
                    modifier = Modifier
                        .size(61.dp)
                        .align(Alignment.CenterStart)
                        .graphicsLayer { rotationZ = -11f; translationY = 8f },
                    contentScale = ContentScale.Fit
                )
            }
            Image(
                painter = painterResource(category.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(76.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { rotationZ = 7f; translationY = -3f },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: MenuCategory, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics {
                    contentDescription = "Volver a las categorías"
                    role = Role.Button
                },
            shape = CircleShape,
            color = Color(0xFFFFD45B),
            shadowElevation = 8.dp,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF54406A),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = category.title,
                color = Color(0xFF174E82),
                fontSize = 27.sp,
                lineHeight = 32.sp,
                fontFamily = AdaRounded,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = category.hint,
                color = Color(0xFF526F72),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ActivityCard(activity: MenuActivity, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "activity-${activity.title}")
    val bob by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700 + (activity.title.length % 4) * 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activity-bob-${activity.title}"
    )
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .height(158.dp)
            .graphicsLayer { translationY = bob }
            .clip(shape)
            .clickable(role = Role.Button, onClick = activity.onClick)
            .semantics {
                contentDescription = "Abrir ${activity.title}"
                role = Role.Button
            },
        shape = shape,
        color = activity.tint,
        shadowElevation = 11.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.84f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(activity.tint, Color.White.copy(alpha = 0.14f))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 13.dp, y = (-15).dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (activity.visual) {
                        MenuVisual.Crayons -> CrayonBundle()
                        MenuVisual.ColorDots -> ColorDotPalette()
                        MenuVisual.Image -> Image(
                            painter = painterResource(activity.imageRes),
                            contentDescription = null,
                            modifier = if (activity.isPortraitCard) {
                                Modifier
                                    .width(64.dp)
                                    .height(91.dp)
                                    .graphicsLayer { translationY = bob }
                            } else {
                                Modifier
                                    .size(91.dp)
                                    .graphicsLayer { translationY = bob }
                            },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activity.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    color = Color(0xFF29495C),
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontFamily = AdaRounded,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun CrayonBundle() {
    val colors = listOf(Color(0xFFFF4D65), Color(0xFFFFC83D), Color(0xFF19B8E6), Color(0xFF8A54DC))
    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        colors.forEachIndexed { index, color ->
            val rotations = listOf(-24f, -8f, 9f, 24f)
            val xOffsets = listOf((-24).dp, (-8).dp, 8.dp, 24.dp)
            val yOffsets = listOf(5.dp, (-2).dp, (-2).dp, 5.dp)
            Box(
                modifier = Modifier
                    .offset(x = xOffsets[index], y = yOffsets[index])
                    .graphicsLayer { rotationZ = rotations[index] }
                    .width(21.dp)
                    .height(69.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.76f), color, color.copy(alpha = 0.78f))))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .width(3.dp)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.50f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.14f))
                )
            }
        }
    }
}

@Composable
private fun ColorDotPalette() {
    val colors = listOf(
        Color(0xFFFF4C5D), Color(0xFFFFB52E), Color(0xFFFFE14B),
        Color(0xFF54CE69), Color(0xFF36B8F0), Color(0xFF8C55E8), Color(0xFFFF65B0)
    )
    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        colors.forEachIndexed { index, color ->
            val positions = listOf(
                (-25).dp to (-19).dp, (0).dp to (-29).dp, 25.dp to (-18).dp,
                (-29).dp to 10.dp, 0.dp to 0.dp, 28.dp to 10.dp, (0).dp to 28.dp
            )
            Box(
                modifier = Modifier
                    .offset(x = positions[index].first, y = positions[index].second)
                    .size(if (index == 4) 22.dp else 18.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.86f), color)),
                        CircleShape
                    )
            )
        }
    }
}
