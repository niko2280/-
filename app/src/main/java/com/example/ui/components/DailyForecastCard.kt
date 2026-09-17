package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.RainModerate
import com.example.ui.theme.SkyBluePrimary

@Composable
fun DailyForecastCard(
    dailyList: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Прогноз на 7 дней",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Global min/max to normalize the temperature bars
            val globalMin = dailyList.minOfOrNull { it.minTemp } ?: 5
            val globalMax = dailyList.maxOfOrNull { it.maxTemp } ?: 25
            val range = (globalMax - globalMin).coerceAtLeast(1)

            for (day in dailyList) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day name (width ~ 70dp)
                    Text(
                        text = day.dayOfWeek,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (day.dayOfWeek == "Сегодня") SkyBluePrimary else Color.White,
                        modifier = Modifier.width(76.dp)
                    )

                    // Weather icon & rain probability
                    Row(
                        modifier = Modifier.width(54.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getWeatherIcon(day.weatherCode),
                            contentDescription = day.conditionDescription,
                            tint = getWeatherIconTint(day.weatherCode),
                            modifier = Modifier.size(22.dp)
                        )
                        if (day.precipitationProb >= 25) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${day.precipitationProb}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = RainModerate
                            )
                        }
                    }

                    // Min Temp
                    val minSign = if (day.minTemp > 0) "+" else ""
                    Text(
                        text = "$minSign${day.minTemp}°",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )

                    // Temperature Visual Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF334155))
                    ) {
                        val leftFraction = ((day.minTemp - globalMin).toFloat() / range).coerceIn(0f, 0.8f)
                        val rightFraction = ((day.maxTemp - globalMin).toFloat() / range).coerceIn(0.2f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(rightFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(SkyBluePrimary, Color(0xFFF59E0B))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Max Temp
                    val maxSign = if (day.maxTemp > 0) "+" else ""
                    Text(
                        text = "$maxSign${day.maxTemp}°",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}
