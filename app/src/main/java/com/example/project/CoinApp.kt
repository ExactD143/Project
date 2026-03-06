package com.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class CoinSide { HEADS, TAILS }
enum class AppScreen { SETUP, TABLE, FLIPPING, RESULT }

const val DEFAULT_HEADS = "Прогуляти пари"
const val DEFAULT_TAILS = "Піти на пари"

@Composable
fun CoinApp(registerShake: ((Float) -> Unit) -> Unit, vibrate: (Long) -> Unit) {
    var screen by remember { mutableStateOf(AppScreen.SETUP) }
    var headsLabel by remember { mutableStateOf("") }
    var tailsLabel by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CoinSide?>(null) }
    var flipForce by remember { mutableStateOf(0f) }
    var busy by remember { mutableStateOf(false) }

    // Ефективні значення: якщо поля порожні — використовуємо дефолт
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
        modifier = Modifier.fillMaxSize().background(
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
