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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FullWeatherState
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SkyBluePrimary
import java.util.Locale

@Composable
fun AtmosphericDetailsGrid(
    weather: FullWeatherState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Атмосферные показатели",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wind Card
            DetailMetricCard(
                title = "Ветер и порывы",
                value = String.format(Locale.getDefault(), "%.1f м/с", weather.windSpeedMs),
                subtitle = "Порывы до ${String.format(Locale.getDefault(), "%.1f", weather.windGustsMs)} м/с",
                icon = Icons.Default.Air,
                iconTint = SkyBluePrimary,
                extraContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Направление",
                            tint = SkyBluePrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(weather.windDirectionDeg.toFloat())
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = getWindDirectionName(weather.windDirectionDeg),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Pressure Card (in mm Hg & hPa)
            DetailMetricCard(
                title = "Давление",
                value = "${weather.pressureMmHg} мм",
                subtitle = "${weather.pressureHpa.toInt()} гПа (норма)",
                icon = Icons.Default.Compress,
                iconTint = Color(0xFFA855F7),
                extraContent = {
                    Text(
                        text = if (weather.pressureMmHg > 762) "Повышенное" else if (weather.pressureMmHg < 755) "Пониженное" else "В норме",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Humidity Card
            DetailMetricCard(
                title = "Влажность",
                value = "${weather.humidity}%",
                subtitle = "Точка росы ~${weather.currentTemp - ((100 - weather.humidity) / 5)}°",
                icon = Icons.Default.WaterDamage,
                iconTint = Color(0xFF06B6D4),
                extraContent = {
                    Text(
                        text = if (weather.humidity > 80) "Высокая" else if (weather.humidity < 40) "Сухой воздух" else "Комфортно",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // Cloud Cover / Sun
            DetailMetricCard(
                title = "Облачность",
                value = "${weather.cloudCoverPct}%",
                subtitle = "УФ-индекс: ${weather.uvIndex.toInt()} (низкий)",
                icon = Icons.Default.WbCloudy,
                iconTint = Color(0xFFFBBF24),
                extraContent = {
                    Text(
                        text = "Восход ${weather.sunrise} • Заход ${weather.sunset}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    extraContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            extraContent()
        }
    }
}

private fun getWindDirectionName(degrees: Int): String {
    return when ((degrees + 22) % 360 / 45) {
        0 -> "С"
        1 -> "СВ"
        2 -> "В"
        3 -> "ЮВ"
        4 -> "Ю"
        5 -> "ЮЗ"
        6 -> "З"
        7 -> "СЗ"
        else -> "Перем."
    }
}
