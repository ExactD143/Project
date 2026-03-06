package com.example.project

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Анімація підкидання ───────────────────────────────────────────────────────
@Composable
fun FlippingScreen(
    strength: Float,
    headsLabel: String,
    tailsLabel: String,
    onFinished: (CoinSide) -> Unit
) {
    val coinResult = remember { if (Math.random() > 0.5) CoinSide.HEADS else CoinSide.TAILS }
    val flipCount  = remember { 3 + (strength * 5).toInt() }

    val scale    = remember { Animatable(1f) }
    val rotY     = remember { Animatable(0f) }
    val rotZ     = remember { Animatable(0f) }
    val shadowA  = remember { Animatable(0.4f) }
    val shadowSc = remember { Animatable(1f) }
    var showGreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val peakScale = 1f + strength * 2.0f
        val riseTime  = (250 - strength * 80).toInt().coerceIn(120, 280)
        val tiltAngle = (if (Math.random() > 0.5) 1 else -1) * (5f + strength * 22f)

        // Підліт
        launch { scale.animateTo(peakScale, tween(riseTime, easing = EaseOutCubic)) }
        launch { shadowA.animateTo(0.08f, tween(riseTime)); shadowSc.animateTo(0.3f, tween(riseTime)) }
        launch { rotZ.animateTo(tiltAngle, tween(riseTime, easing = EaseOutSine)) }
        delay(riseTime.toLong())

        // Кручення
        var totalRot = 0f
        repeat(flipCount) {
            val fd = (75 + (1f - strength) * 80).toInt()
            totalRot += 180f
            rotY.animateTo(totalRot, tween(fd, easing = LinearEasing))
            showGreen = (totalRot / 180).toInt() % 2 == 0
            delay(4)
        }
        val finalRot = if (coinResult == CoinSide.HEADS) Math.round(totalRot / 360f) * 360f
                       else Math.round(totalRot / 360f) * 360f + 180f
        rotY.animateTo(finalRot, tween(110, easing = EaseOutSine))
        showGreen = coinResult == CoinSide.HEADS

        // Падіння
        val fallTime = (riseTime * 0.9f).toInt()
        launch { scale.animateTo(1.08f, tween(fallTime, easing = EaseInCubic)) }
        launch { shadowA.animateTo(0.4f, tween(fallTime)); shadowSc.animateTo(1f, tween(fallTime)) }
        launch { rotZ.animateTo(0f, tween(fallTime, easing = EaseOutSine)) }
        delay(fallTime.toLong())

        // Відскок
        scale.animateTo(1.15f, tween(55, easing = EaseOutCubic))
        scale.animateTo(1f, tween(200, easing = EaseOutBounce))
        delay(150)
        onFinished(coinResult)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(86.dp, 14.dp)
                .graphicsLayer {
                    translationY = 44.dp.toPx()
                    alpha = shadowA.value
                    scaleX = shadowSc.value
                    scaleY = shadowSc.value
                }
                .background(Brush.radialGradient(listOf(Color(0xCC000000), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.size(90.dp).graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                rotationY = rotY.value; rotationZ = rotZ.value
                cameraDistance = 9f * density
            }
        ) {
            Coin(
                isGreen = showGreen,
                label = if (showGreen) headsLabel else tailsLabel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Компонент монети ──────────────────────────────────────────────────────────
@Composable
fun Coin(isGreen: Boolean, label: String, modifier: Modifier = Modifier) {
    val light = if (isGreen) Color(0xFF66BB6A) else Color(0xFFEF5350)
    val main  = if (isGreen) Color(0xFF388E3C) else Color(0xFFC62828)
    val dark  = if (isGreen) Color(0xFF1B5E20) else Color(0xFF7F0000)
    val rimA  = if (isGreen) Color(0xFF81C784) else Color(0xFFEF9A9A)
    val rimB  = if (isGreen) Color(0xFF2E7D32) else Color(0xFFB71C1C)

    Box(
        modifier = modifier
            .shadow(14.dp, CircleShape, spotColor = dark)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(light, main, dark), radius = 300f))
            .border(3.5.dp, Brush.sweepGradient(listOf(rimA, rimB, rimA, rimB, rimA)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Блик (імітація опуклості)
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(
                Brush.radialGradient(
                    listOf(Color(0x2FFFFFFF), Color.Transparent),
                    center = Offset(300f, 200f),
                    radius = 380f
                )
            )
        )
        if (label.isNotBlank())
            Text(
                label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
    }
}
