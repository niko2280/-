package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrecipitationType
import com.example.data.model.RainNowcast
import com.example.ui.theme.RainDownpour
import com.example.ui.theme.RainDrizzle
import com.example.ui.theme.RainHeavy
import com.example.ui.theme.RainModerate
import com.example.ui.theme.SkyBluePrimary
import java.util.Locale

@Composable
fun RainNowcastCard(
    nowcast: RainNowcast,
    onOpenRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = if (nowcast.isRaining) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF132F52), Color(0xFF0D1E36))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            nowcast.isRaining && nowcast.willIntensify -> Color(0xFFF97316)
            nowcast.isRaining -> SkyBluePrimary
            nowcast.minutesUntilStart != null -> Color(0xFFFBBF24)
            else -> Color(0xFF34D399)
        },
        animationSpec = tween(400),
        label = "status_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rain_nowcast_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row with rain status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Осадки",
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Осадки в реальном времени",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (nowcast.isRaining) {
                                    String.format(Locale.getDefault(), "%.1f мм/ч", nowcast.currentRateMmH)
                                } else "0.0 мм/ч",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Indicator chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (nowcast.isRaining) "Дождь сейчас" else "Сухо",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Headline: "Дождь прекратится через ~35 минут"
                Text(
                    text = nowcast.summaryText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = nowcast.detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 15-Minute Timeline Bar Chart
                if (nowcast.timelinePoints.isNotEmpty()) {
                    Text(
                        text = "Прогноз осадков по минутам (следующие 2 часа):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxPrecip = nowcast.timelinePoints.maxOfOrNull { it.precipitationMm }?.coerceAtLeast(2.0) ?: 2.0

                        for (step in nowcast.timelinePoints) {
                            val barHeightFraction by animateFloatAsState(
                                targetValue = (step.precipitationMm / maxPrecip).toFloat().coerceIn(0.08f, 1.0f),
                                animationSpec = tween(500),
                                label = "bar_height"
                            )

                            val barColor = when (step.intensity) {
                                PrecipitationType.NONE -> Color(0xFF334155)
                                PrecipitationType.DRIZZLE -> RainDrizzle
                                PrecipitationType.RAIN -> RainModerate
                                PrecipitationType.HEAVY_RAIN -> RainHeavy
                                else -> RainDownpour
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Rain amount text above bar
                                if (step.precipitationMm > 0.05) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f", step.precipitationMm),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = barColor
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(11.dp))
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Bar
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height((40 * barHeightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Time label
                                Text(
                                    text = step.label.replace(" мин", "м").replace("Сейчас", "0м"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action button to open full country atmospheric radar
                FilledTonalButton(
                    onClick = onOpenRadar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_radar_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SkyBluePrimary.copy(alpha = 0.18f),
                        contentColor = SkyBluePrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Интерактивная карта осадков Беларуси",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
