package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BelarusBoundary
import com.example.data.model.BelarusCity
import com.example.data.model.FullWeatherState
import com.example.data.model.RadarTimelineSnapshot
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.RainDownpour
import com.example.ui.theme.RainDrizzle
import com.example.ui.theme.RainHeavy
import com.example.ui.theme.RainModerate
import com.example.ui.theme.SkyBluePrimary
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class RadarLayer {
    PRECIPITATION, CLOUDS, WIND
}

@Composable
fun RadarMapScreen(
    weatherState: FullWeatherState,
    radarFrames: List<RadarTimelineSnapshot>,
    selectedCity: BelarusCity,
    onCitySelected: (BelarusCity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderMinutes by remember { mutableFloatStateOf(0f) } // -30f to +120f
    var isPlaying by remember { mutableStateOf(false) }
    var activeLayer by remember { mutableStateOf(RadarLayer.PRECIPITATION) }
    var inspectedCity by remember { mutableStateOf(selectedCity) }

    // Auto playback loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1200)
            val next = sliderMinutes + 15f
            sliderMinutes = if (next > 120f) -30f else next
        }
    }

    // Sync inspected city with selected city initially
    LaunchedEffect(selectedCity) {
        inspectedCity = selectedCity
    }

    // Find current closest radar frame
    val currentFrame = remember(sliderMinutes, radarFrames) {
        val target = sliderMinutes.toInt()
        radarFrames.minByOrNull { kotlin.math.abs(it.minuteOffset - target) } ?: radarFrames.firstOrNull()
    }

    // Calculate precipitation at inspected city for current slider position
    val cityPrecipitation = remember(sliderMinutes, inspectedCity, currentFrame) {
        val frame = currentFrame
        if (frame != null) {
            val cell = frame.cells.minByOrNull {
                hypot(it.centerLat - inspectedCity.latitude, it.centerLon - inspectedCity.longitude)
            }
            if (cell != null) {
                val dist = hypot(cell.centerLat - inspectedCity.latitude, cell.centerLon - inspectedCity.longitude) * 111.0 // km
                if (dist < cell.radiusKm) {
                    (cell.intensityMmH * (1.0 - (dist / cell.radiusKm) * 0.7)).toFloat().coerceAtLeast(0.0f)
                } else 0.0f
            } else 0.0f
        } else 0.0f
    }

    val liveStatusText = remember(sliderMinutes, cityPrecipitation, inspectedCity) {
        val min = sliderMinutes.toInt()
        val cityName = inspectedCity.name
        when {
            min <= 0 && cityPrecipitation > 0.05f -> {
                val stopsIn = (35 - min).coerceAtLeast(10)
                "$cityName: идет дождь (${String.format(Locale.getDefault(), "%.1f", cityPrecipitation)} мм/ч). Прекратится через ~$stopsIn мин."
            }
            min <= 0 -> {
                "$cityName: сейчас без осадков. Облачность умеренная."
            }
            cityPrecipitation > 3.0f -> {
                "$cityName (+$min мин): сильный дождь, интенсивность ${String.format(Locale.getDefault(), "%.1f", cityPrecipitation)} мм/ч!"
            }
            cityPrecipitation > 0.1f -> {
                "$cityName (+$min мин): осадки интенсивностью ${String.format(Locale.getDefault(), "%.1f", cityPrecipitation)} мм/ч."
            }
            else -> {
                "$cityName (+$min мин): осадки прекратились. Без дождя."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070F1E))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Layer switch chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeLayer == RadarLayer.PRECIPITATION,
                onClick = { activeLayer = RadarLayer.PRECIPITATION },
                label = { Text("Осадки (Радар)", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBluePrimary,
                    selectedLabelColor = Color(0xFF00354E)
                )
            )

            FilterChip(
                selected = activeLayer == RadarLayer.CLOUDS,
                onClick = { activeLayer = RadarLayer.CLOUDS },
                label = { Text("Облачность", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF94A3B8),
                    selectedLabelColor = Color(0xFF0F172A)
                )
            )

            FilterChip(
                selected = activeLayer == RadarLayer.WIND,
                onClick = { activeLayer = RadarLayer.WIND },
                label = { Text("Ветер", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF00354E)
                )
            )
        }

        // Live Radar Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (cityPrecipitation > 0.1f) SkyBluePrimary else Color(0xFF34D399))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = liveStatusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        text = "Коснитесь любого города на карте для проверки осадков",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Canvas Map of Belarus & Atmospheric Radar
        val textMeasurer = rememberTextMeasurer()
        val boundary = remember { BelarusBoundary() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                .background(Color(0xFF0B1426))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val mapW = size.width
                            val mapH = size.height
                            // Find clicked city
                            val nearest = BelarusCity.ALL.minByOrNull { city ->
                                val (nx, ny) = boundary.toNormalized(city.latitude, city.longitude)
                                val cx = nx * mapW
                                val cy = ny * mapH
                                hypot(cx - tapOffset.x, cy - tapOffset.y)
                            }
                            if (nearest != null) {
                                inspectedCity = nearest
                                onCitySelected(nearest)
                            }
                        }
                    }
                    .testTag("belarus_radar_canvas")
            ) {
                val mapWidth = size.width
                val mapHeight = size.height

                // Draw regional boundaries of Belarus (approx polygon vertices)
                // Belarus stylized geo border:
                val borderPath = Path().apply {
                    val pts = listOf(
                        Pair(55.8, 27.5), Pair(56.15, 28.2), Pair(55.9, 30.5), Pair(55.2, 31.0),
                        Pair(54.0, 31.8), Pair(53.4, 32.7), Pair(52.6, 31.7), Pair(52.0, 31.4),
                        Pair(51.3, 30.5), Pair(51.8, 29.5), Pair(51.8, 27.5), Pair(51.6, 24.5),
                        Pair(52.0, 23.6), Pair(52.6, 23.4), Pair(53.8, 23.7), Pair(54.4, 25.6),
                        Pair(54.9, 26.0), Pair(55.7, 26.6)
                    )
                    var first = true
                    for (pt in pts) {
                        val (nx, ny) = boundary.toNormalized(pt.first, pt.second)
                        val x = nx * mapWidth
                        val y = ny * mapHeight
                        if (first) {
                            moveTo(x, y)
                            first = false
                        } else {
                            lineTo(x, y)
                        }
                    }
                    close()
                }

                // Background inside Belarus territory
                drawPath(
                    path = borderPath,
                    color = Color(0xFF13233C)
                )
                // Border glow
                drawPath(
                    path = borderPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    style = Stroke(width = 2.5f)
                )

                // Draw Internal Oblast division lines
                val (minskX, minskY) = boundary.toNormalized(53.9045, 27.5615)
                val (brestX, brestY) = boundary.toNormalized(52.0976, 23.7341)
                val (grodnoX, grodnoY) = boundary.toNormalized(53.6694, 23.8131)
                val (vitebskX, vitebskY) = boundary.toNormalized(55.1904, 30.2049)
                val (gomelX, gomelY) = boundary.toNormalized(52.4345, 30.9754)
                val (mogilevX, mogilevY) = boundary.toNormalized(53.9007, 30.3313)

                // Atmospheric front / precipitation overlay
                val frame = currentFrame
                if (frame != null && (activeLayer == RadarLayer.PRECIPITATION || activeLayer == RadarLayer.CLOUDS)) {
                    for (cell in frame.cells) {
                        val (cxNorm, cyNorm) = boundary.toNormalized(cell.centerLat, cell.centerLon)
                        val cellPxX = cxNorm * mapWidth
                        val cellPxY = cyNorm * mapHeight
                        val radiusPx = (cell.radiusKm / 500f) * mapWidth

                        val color = if (activeLayer == RadarLayer.CLOUDS) {
                            Color(0xFFE2E8F0).copy(alpha = cell.cloudDensity * 0.45f)
                        } else {
                            when {
                                cell.intensityMmH < 0.8f -> RainDrizzle.copy(alpha = 0.5f)
                                cell.intensityMmH < 2.5f -> RainModerate.copy(alpha = 0.65f)
                                cell.intensityMmH < 5.0f -> RainHeavy.copy(alpha = 0.75f)
                                else -> RainDownpour.copy(alpha = 0.85f)
                            }
                        }

                        // Gradient precipitation blob
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color, color.copy(alpha = 0.2f), Color.Transparent),
                                center = Offset(cellPxX, cellPxY),
                                radius = radiusPx
                            ),
                            radius = radiusPx,
                            center = Offset(cellPxX, cellPxY)
                        )
                    }
                }

                // Wind vector overlay
                if (activeLayer == RadarLayer.WIND) {
                    val stepX = mapWidth / 6
                    val stepY = mapHeight / 6
                    for (i in 1..5) {
                        for (j in 1..5) {
                            val arrowX = i * stepX
                            val arrowY = j * stepY
                            // Wind direction ~ 60 deg (South-West to North-East)
                            val angleRad = Math.toRadians(60.0)
                            val len = 18f
                            val endX = arrowX + (cos(angleRad) * len).toFloat()
                            val endY = arrowY - (sin(angleRad) * len).toFloat()

                            drawLine(
                                color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                                start = Offset(arrowX, arrowY),
                                end = Offset(endX, endY),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // Draw Belarusian Cities
                for (city in BelarusCity.ALL) {
                    val (nx, ny) = boundary.toNormalized(city.latitude, city.longitude)
                    val cityX = nx * mapWidth
                    val cityY = ny * mapHeight

                    val isSelected = city.id == inspectedCity.id
                    val isCapital = city.isCapital

                    // Halo for selected city
                    if (isSelected) {
                        drawCircle(
                            color = SkyBluePrimary.copy(alpha = 0.35f),
                            radius = 16f,
                            center = Offset(cityX, cityY)
                        )
                    }

                    // City pin point
                    val pointColor = when {
                        isSelected -> Color(0xFFF59E0B)
                        isCapital -> Color(0xFF38BDF8)
                        city.isOblastCenter -> Color(0xFFE2E8F0)
                        else -> Color(0xFF94A3B8)
                    }

                    drawCircle(
                        color = pointColor,
                        radius = if (isCapital) 7f else if (city.isOblastCenter) 5f else 3.5f,
                        center = Offset(cityX, cityY)
                    )

                    // Draw city label for capital and oblast centers
                    if (isCapital || city.isOblastCenter || isSelected) {
                        val textLayoutResult = textMeasurer.measure(
                            text = city.name,
                            style = TextStyle(
                                fontSize = if (isCapital) 12.sp else 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFF8FAFC)
                            )
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(cityX + 8f, cityY - 8f)
                        )
                    }
                }
            }

            // Legend at bottom left of canvas
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Интенсивность осадков:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegendItem("0.1", RainDrizzle)
                    LegendItem("1.5", RainModerate)
                    LegendItem("4.0", RainHeavy)
                    LegendItem("8+", RainDownpour)
                    Text("мм/ч", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                }
            }

            // Country Title watermark
            Text(
                text = "БЕЛАРУСЬ • РАДАР",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Timeline Slider Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Header of Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkyBluePrimary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                                tint = Color(0xFF00354E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Таймлайн перемещения осадков",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val currentMinutesInt = sliderMinutes.toInt()
                            val displayTime = when {
                                currentMinutesInt < 0 -> "${-currentMinutesInt} мин назад"
                                currentMinutesInt == 0 -> "СЕЙЧАС (0 мин)"
                                else -> "+$currentMinutesInt минут вперед"
                            }
                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (currentMinutesInt == 0) SkyBluePrimary else Color.White
                            )
                        }
                    }

                    // Reset button to NOW (0)
                    IconButton(
                        onClick = {
                            sliderMinutes = 0f
                            isPlaying = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Сброс на Сейчас",
                            tint = SkyBluePrimary
                        )
                    }
                }

                // Continuous Scrubbing Slider
                Slider(
                    value = sliderMinutes,
                    onValueChange = {
                        sliderMinutes = it
                        isPlaying = false
                    },
                    valueRange = -30f..120f,
                    steps = 9, // -30, -15, 0, 15, 30, 45, 60, 75, 90, 105, 120
                    colors = SliderDefaults.colors(
                        thumbColor = SkyBluePrimary,
                        activeTrackColor = SkyBluePrimary,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("radar_timeline_slider")
                )

                // Timeline Tick Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-30м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-15м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("СЕЙЧАС", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = SkyBluePrimary)
                    Text("+30м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+60м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+90м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+120м", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color(0xFFCBD5E1)
        )
    }
}
