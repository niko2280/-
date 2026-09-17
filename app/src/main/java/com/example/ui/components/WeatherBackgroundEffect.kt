package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class RainParticle(
    val initialX: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float
)

@Composable
fun WeatherBackgroundEffect(
    isRaining: Boolean,
    isThunderstorm: Boolean,
    isOvercast: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")

    val rainProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_fall"
    )

    val thunderFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thunder"
    )

    // Generate random raindrops once
    val particles = remember {
        List(70) {
            RainParticle(
                initialX = Random.nextFloat(),
                speed = 0.8f + Random.nextFloat() * 0.7f,
                length = 20f + Random.nextFloat() * 30f,
                alpha = 0.25f + Random.nextFloat() * 0.45f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background gradient
        val bgBrush = when {
            isThunderstorm -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0B132B))
            )
            isRaining -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0C2442), Color(0xFF133E68), Color(0xFF0B1B2B))
            )
            isOvercast -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A))
            )
            else -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0B2545), Color(0xFF134074), Color(0xFF07101E))
            )
        }
        drawRect(brush = bgBrush)

        // Lightning flash effect
        if (isThunderstorm && thunderFlash > 0.88f) {
            val flashAlpha = if (thunderFlash > 0.94f) 0.28f else 0.12f
            drawRect(color = Color.White.copy(alpha = flashAlpha))
        }

        // Falling raindrops
        if (isRaining) {
            for (p in particles) {
                val currentY = ((p.initialX * 2000f + rainProgress * height * p.speed) % height)
                val startX = (p.initialX * width + (rainProgress * 50f)) % width
                val endX = startX - 6f
                val endY = currentY + p.length

                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = p.alpha),
                    start = Offset(startX, currentY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.8f
                )
            }
        }
    }
}
