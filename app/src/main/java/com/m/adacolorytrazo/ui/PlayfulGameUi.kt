package com.m.adacolorytrazo.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.m.adacolorytrazo.R

private val PlayfulInk = Color(0xFF24495A)
private val PlayfulTeal = Color(0xFF246C70)
private val PlayfulMint = Color(0xFF53B884)
private val PlayfulGold = Color(0xFFFFD66D)

val PlayfulCardTints = listOf(
    Color(0xFFFFE2AA),
    Color(0xFFD8F1D4),
    Color(0xFFD5ECFF),
    Color(0xFFFFDCE5),
    Color(0xFFE9DFFF)
)

/** Fondo común del mundo educativo: parque 3D colorido con espacio para la interacción. */
@Composable
fun PlayfulGameBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ada_menu_educational_park),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1ECAEFFF),
                            Color.Transparent,
                            Color(0x1A17391F)
                        )
                    )
                )
        )
    }
}

@Composable
fun AdventureTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                color = PlayfulInk,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = PlayfulTeal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(8.dp))
        PlayfulHomeButton(onClick = onBack)
    }
}

@Composable
fun PlayfulHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Volver al menú"
) {
    Surface(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = PlayfulGold,
        contentColor = PlayfulTeal,
        shadowElevation = 8.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.96f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun AdventureTargetCard(
    eyebrow: String,
    targetName: String,
    illustrationRes: Int,
    instruction: String,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    showInstruction: Boolean = true,
    showTargetName: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(130.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFFFFCF3),
        shadowElevation = 12.dp,
        tonalElevation = 3.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFFFE8B5), Color(0xFFFFD98B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val transition = rememberInfiniteTransition(label = "target-float")
                val bob by transition.animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1450, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "target-bob"
                )
                val density = LocalDensity.current
                Image(
                    painter = painterResource(illustrationRes),
                    contentDescription = targetName,
                    modifier = Modifier
                        .size(101.dp)
                        .graphicsLayer { translationY = bob * density.density },
                    contentScale = ContentScale.Fit
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (showTargetName) {
                    Text(
                        text = eyebrow.uppercase(),
                        color = Color(0xFF72827B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = targetName,
                        color = PlayfulInk,
                        fontSize = if (targetName.length > 10) 20.sp else 24.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showInstruction) {
                        Text(
                            text = instruction,
                            color = PlayfulTeal,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👂",
                            fontSize = 38.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Escucha el nombre y toca la imagen igual"
                            }
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onRepeat)
                    .semantics { contentDescription = "Escuchar la consigna" },
                shape = CircleShape,
                color = PlayfulGold,
                contentColor = Color(0xFF624B1A),
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔊", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AdventureSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 5.dp, top = 1.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = PlayfulTeal
    )
}

@Composable
fun AdventureOptionTile(
    title: String,
    illustrationRes: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String = title,
    showTitle: Boolean = true
) {
    val shape = RoundedCornerShape(25.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        shape = shape,
        color = Color.White,
        contentColor = PlayfulInk,
        shadowElevation = if (enabled) 9.dp else 3.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) Color.White else Color(0xFFE5E7E5)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val transition = rememberInfiniteTransition(label = "option-float-$title")
            val bob by transition.animateFloat(
                initialValue = -2.5f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1550, delayMillis = (title.length % 5) * 95, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "option-bob-$title"
            )
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(tint.copy(alpha = 0.82f), Color.White.copy(alpha = 0.65f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                )
                Image(
                    painter = painterResource(illustrationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer { translationY = bob * density.density },
                    contentScale = ContentScale.Fit
                )
            }
            if (showTitle) {
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 4.dp),
                    color = PlayfulInk,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AdventureStatusCard(
    message: String,
    isCorrect: Boolean?,
    modifier: Modifier = Modifier,
    visualOnly: Boolean = false
) {
    val tone = when (isCorrect) {
        true -> Color(0xFFE4F4D7)
        false -> Color(0xFFFFE6DD)
        null -> Color(0xFFFFFCF3)
    }
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(18.dp),
        color = tone,
        contentColor = PlayfulInk,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp)
                .semantics { contentDescription = message },
            contentAlignment = Alignment.Center
        ) {
            if (visualOnly) {
                Text(
                    text = when (isCorrect) {
                        true -> "⭐✨⭐"
                        false -> "👀🔄"
                        null -> "🌼"
                    },
                    fontSize = 24.sp
                )
            } else {
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlayfulActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = PlayfulGold
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = accent,
        contentColor = PlayfulInk,
        shadowElevation = if (enabled) 5.dp else 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.88f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
