package com.example.project

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Моделі ────────────────────────────────────────────────────────────────────

enum class CoinSide { HEADS, TAILS }
enum class AppScreen { SETUP, TABLE, FLIPPING, RESULT }

private const val DEFAULT_HEADS = "Піти на пари"
private const val DEFAULT_TAILS = "Прогуляти пари"

// ── Головна навігація ─────────────────────────────────────────────────────────

@Composable
fun CoinApp(registerShake: ((Float) -> Unit) -> Unit, vibrate: (Long) -> Unit) {
    var screen by remember { mutableStateOf(AppScreen.SETUP) }
    var headsLabel by remember { mutableStateOf("") }
    var tailsLabel by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CoinSide?>(null) }
    var flipForce by remember { mutableStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    val effectiveHeads = headsLabel.ifBlank { DEFAULT_HEADS }
    val effectiveTails = tailsLabel.ifBlank { DEFAULT_TAILS }

    LaunchedEffect(Unit) {
        registerShake { force ->
            if (screen == AppScreen.TABLE && !busy) {
                flipForce = (force / 28f).coerceIn(0.3f, 1f)
                busy = true
                screen = AppScreen.FLIPPING
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1A3320), Color(0xFF0F1F13), Color(0xFF080E09)),
                    radius = 1600f
                )
            )
    ) {
        when (screen) {
            AppScreen.SETUP -> SetupScreen(
                headsValue = headsLabel,
                tailsValue = tailsLabel,
                onHeadsChange = { headsLabel = it },
                onTailsChange = { tailsLabel = it },
                onConfirm = { screen = AppScreen.TABLE }
            )
            AppScreen.TABLE -> TableScreen(effectiveHeads, effectiveTails)
            AppScreen.FLIPPING -> FlippingScreen(
                strength = flipForce,
                headsLabel = effectiveHeads,
                tailsLabel = effectiveTails
            ) { coinResult ->
                vibrate((80 + flipForce * 130).toLong())
                result = coinResult
                busy = false
                screen = AppScreen.RESULT
            }
            AppScreen.RESULT -> ResultScreen(
                result = result!!,
                headsLabel = effectiveHeads,
                tailsLabel = effectiveTails,
                onPlayAgain = { screen = AppScreen.TABLE },
                onNewGame = { screen = AppScreen.SETUP }
            )
        }
    }
}

// ── Екран налаштувань ─────────────────────────────────────────────────────────

@Composable
fun SetupScreen(
    headsValue: String,
    tailsValue: String,
    onHeadsChange: (String) -> Unit,
    onTailsChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Рандомайзер",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD700),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Вкажи що означає кожна сторона\n(або залиш порожнім — буде стандарт)",
            fontSize = 13.sp,
            color = Color(0xFFB0C4B0),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        LabelInput("ЗЕЛЕНА сторона", Color(0xFF4CAF50), headsValue, DEFAULT_HEADS, onHeadsChange)
        Spacer(Modifier.height(20.dp))
        LabelInput("ЧЕРВОНА сторона", Color(0xFFE53935), tailsValue, DEFAULT_TAILS, onTailsChange)

        Spacer(Modifier.height(44.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Підкинути 🎲", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LabelInput(
    label: String,
    color: Color,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x20FFFFFF))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = Color(0xFF666666), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = color,
                unfocusedBorderColor = Color(0x40FFFFFF),
                cursorColor = color
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

// ── Екран столу ───────────────────────────────────────────────────────────────

@Composable
fun TableScreen(headsLabel: String, tailsLabel: String) {
    val inf = rememberInfiniteTransition(label = "idle")
    val idleRotY by inf.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "ry"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            "Потрясіть телефон щоб підкинути монету",
            color = Color(0xFF8AAF8A),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp, start = 40.dp, end = 40.dp)
        )

        // Тінь
        Box(
            Modifier
                .size(86.dp, 14.dp)
                .align(Alignment.Center)
                .graphicsLayer { translationY = 44.dp.toPx(); alpha = 0.4f }
                .background(
                    Brush.radialGradient(listOf(Color(0xBB000000), Color.Transparent)),
                    CircleShape
                )
        )

        // Монета
        Box(
            Modifier
                .size(90.dp)
                .align(Alignment.Center)
                .graphicsLayer { rotationY = idleRotY; cameraDistance = 10f * density }
        ) {
            Coin(isGreen = true, label = headsLabel, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 52.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SideBadge("🟢 Зелена", headsLabel, Color(0xFF4CAF50))
            SideBadge("🔴 Червона", tailsLabel, Color(0xFFE53935))
        }
    }
}

@Composable
private fun SideBadge(title: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

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

        launch { scale.animateTo(peakScale, tween(riseTime, easing = EaseOutCubic)) }
        launch { shadowA.animateTo(0.08f, tween(riseTime)); shadowSc.animateTo(0.3f, tween(riseTime)) }
        launch { rotZ.animateTo(tiltAngle, tween(riseTime, easing = EaseOutSine)) }
        delay(riseTime.toLong())

        var totalRot = 0f
        repeat(flipCount) {
            val fd = (75 + (1f - strength) * 80).toInt()
            totalRot += 180f
            rotY.animateTo(totalRot, tween(fd, easing = LinearEasing))
            showGreen = (totalRot / 180).toInt() % 2 == 0
            delay(4)
        }
        val finalRot = if (coinResult == CoinSide.HEADS)
            Math.round(totalRot / 360f) * 360f
        else
            Math.round(totalRot / 360f) * 360f + 180f
        rotY.animateTo(finalRot, tween(110, easing = EaseOutSine))
        showGreen = coinResult == CoinSide.HEADS

        val fallTime = (riseTime * 0.9f).toInt()
        launch { scale.animateTo(1.08f, tween(fallTime, easing = EaseInCubic)) }
        launch { shadowA.animateTo(0.4f, tween(fallTime)); shadowSc.animateTo(1f, tween(fallTime)) }
        launch { rotZ.animateTo(0f, tween(fallTime, easing = EaseOutSine)) }
        delay(fallTime.toLong())

        scale.animateTo(1.15f, tween(55, easing = EaseOutCubic))
        scale.animateTo(1f, tween(200, easing = EaseOutBounce))
        delay(150)
        onFinished(coinResult)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(86.dp, 14.dp)
                .graphicsLayer {
                    translationY = 44.dp.toPx()
                    alpha = shadowA.value
                    scaleX = shadowSc.value
                    scaleY = shadowSc.value
                }
                .background(
                    Brush.radialGradient(listOf(Color(0xCC000000), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(90.dp)
                .graphicsLayer {
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

// ── Екран результату ──────────────────────────────────────────────────────────

@Composable
fun ResultScreen(
    result: CoinSide,
    headsLabel: String,
    tailsLabel: String,
    onPlayAgain: () -> Unit,
    onNewGame: () -> Unit
) {
    val isGreen = result == CoinSide.HEADS
    val label   = if (isGreen) headsLabel else tailsLabel
    val color   = if (isGreen) Color(0xFF4CAF50) else Color(0xFFE53935)

    val animScale = remember { Animatable(0.4f) }
    val animAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            animScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { animAlpha.animateTo(1f, tween(350)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer { alpha = animAlpha.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Результат!", color = Color(0xFF90EE90), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(36.dp))

        Box(Modifier.size(160.dp).graphicsLayer { scaleX = animScale.value; scaleY = animScale.value }) {
            Coin(isGreen = isGreen, label = label, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(28.dp))

        Text(
            if (isGreen) "🟢 Зелена сторона" else "🔴 Червона сторона",
            color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Ще раз!", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90EE90)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF90EE90)),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Змінити варіанти", fontSize = 16.sp) }
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
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
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
