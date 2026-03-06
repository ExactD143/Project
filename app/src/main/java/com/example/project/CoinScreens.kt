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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
        Text("Рандомайзер", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD700), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Вкажи що означає кожна сторона\n(або залиш порожнім — буде стандарт)",
            fontSize = 13.sp, color = Color(0xFFB0C4B0), textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))

        LabelInput(
            label = "ЗЕЛЕНА сторона",
            color = Color(0xFF4CAF50),
            value = headsValue,
            placeholder = DEFAULT_HEADS,
            onChange = onHeadsChange
        )
        Spacer(Modifier.height(20.dp))
        LabelInput(
            label = "ЧЕРВОНА сторона",
            color = Color(0xFFE53935),
            value = tailsValue,
            placeholder = DEFAULT_TAILS,
            onChange = onTailsChange
        )
        Spacer(Modifier.height(44.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700),
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Підкинути 🎲", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun LabelInput(label: String, color: Color, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
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
            value = value, onValueChange = onChange, singleLine = true,
            placeholder = { Text(placeholder, color = Color(0xFF666666), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = color, unfocusedBorderColor = Color(0x40FFFFFF), cursorColor = color
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

// ── Екран столу ───────────────────────────────────────────────────────────────
@Composable
fun TableScreen(headsLabel: String, tailsLabel: String) {
    val inf = rememberInfiniteTransition(label = "idle")
    val idleRotY by inf.animateFloat(-5f, 5f,
        infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), label = "ry")

    Box(modifier = Modifier.fillMaxSize()) {
        Text("Потрясіть телефон щоб підкинути монету",
            color = Color(0xFF8AAF8A), fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 68.dp, start = 40.dp, end = 40.dp))

        Box(Modifier.size(86.dp, 14.dp).align(Alignment.Center)
            .graphicsLayer { translationY = 44.dp.toPx(); alpha = 0.4f }
            .background(Brush.radialGradient(listOf(Color(0xBB000000), Color.Transparent)), CircleShape))

        Box(Modifier.size(90.dp).align(Alignment.Center)
            .graphicsLayer { rotationY = idleRotY; cameraDistance = 10f * density }) {
            Coin(isGreen = true, label = headsLabel, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(bottom = 52.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SideBadge("🟢 Зелена", headsLabel, Color(0xFF4CAF50))
            SideBadge("🔴 Червона", tailsLabel, Color(0xFFE53935))
        }
    }
}

@Composable
fun SideBadge(title: String, label: String, color: Color) {
    Column(
        modifier = Modifier.width(140.dp)
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
    val label = if (isGreen) headsLabel else tailsLabel
    val color = if (isGreen) Color(0xFF4CAF50) else Color(0xFFE53935)

    val animScale = remember { Animatable(0.4f) }
    val animAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { animScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
        launch { animAlpha.animateTo(1f, tween(350)) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).graphicsLayer { alpha = animAlpha.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Результат!", color = Color(0xFF90EE90), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(36.dp))

        Box(Modifier.size(160.dp).graphicsLayer { scaleX = animScale.value; scaleY = animScale.value }) {
            Coin(isGreen = isGreen, label = label, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(28.dp))

        Text(if (isGreen) "🟢 Зелена сторона" else "🔴 Червона сторона",
            color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF1A1A1A)),
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
