package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BelarusCity
import com.example.data.model.FullWeatherState
import com.example.ui.components.AtmosphericDetailsGrid
import com.example.ui.components.CitySelectionSheet
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.HourlyForecastRow
import com.example.ui.components.LocalizedAlertCard
import com.example.ui.components.RainNowcastCard
import com.example.ui.components.WeatherBackgroundEffect
import com.example.ui.components.getWeatherIcon
import com.example.ui.components.getWeatherIconTint
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SkyBluePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherHomeScreen(
    weatherState: FullWeatherState?,
    isLoading: Boolean,
    selectedCity: BelarusCity,
    onCitySelected: (BelarusCity) -> Unit,
    onRefresh: () -> Unit,
    onUseGpsLocation: () -> Unit,
    onOpenRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCitySheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val isRaining = weatherState?.precipitationNowMmH?.let { it > 0.05 } ?: false
    val isThunderstorm = weatherState?.weatherCode in listOf(95, 96, 99)
    val isOvercast = weatherState?.weatherCode in listOf(3, 45, 48)

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        // Animated Canvas Weather Ambience (Raindrops / Cloud glow)
        WeatherBackgroundEffect(
            isRaining = isRaining,
            isThunderstorm = isThunderstorm,
            isOvercast = isOvercast
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar: City Selector & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // City Selector Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .clickable { showCitySheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("city_selector_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Выбор города",
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCity.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Refresh Button
                val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ),
                    label = "rotation"
                )

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .testTag("refresh_weather_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить погоду",
                        tint = SkyBluePrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isLoading) rotation else 0f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Weather Hero
            if (weatherState != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Weather Condition Icon
                    Icon(
                        imageVector = getWeatherIcon(weatherState.weatherCode),
                        contentDescription = weatherState.conditionDescription,
                        tint = getWeatherIconTint(weatherState.weatherCode),
                        modifier = Modifier.size(68.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Temperature
                    val sign = if (weatherState.currentTemp > 0) "+" else ""
                    Text(
                        text = "$sign${weatherState.currentTemp}°",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    // Condition Text
                    Text(
                        text = weatherState.conditionDescription,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Feels like & Min/Max
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val feelsSign = if (weatherState.feelsLike > 0) "+" else ""
                        Text(
                            text = "Ощущается как $feelsSign${weatherState.feelsLike}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "макс +${weatherState.maxTempToday}° / мин +${weatherState.minTempToday}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    val updateTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weatherState.lastUpdated))
                    Text(
                        text = "Обновлено в $updateTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Localized Belarusian Alerts
                if (weatherState.alerts.isNotEmpty()) {
                    for (alert in weatherState.alerts) {
                        LocalizedAlertCard(
                            alert = alert,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Rain Nowcast Card (Exact precipitation mm/h + timeline)
                RainNowcastCard(
                    nowcast = weatherState.nowcast,
                    onOpenRadar = onOpenRadar,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 24-Hour Forecast Row
                HourlyForecastRow(
                    hourlyList = weatherState.hourlyList,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 7-Day Forecast
                DailyForecastCard(
                    dailyList = weatherState.dailyList,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Atmospheric Details (Pressure mmHg, Humidity, Wind, UV)
                AtmosphericDetailsGrid(
                    weather = weatherState,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SkyBluePrimary)
                }
            }
        }
    }

    // City Selection Sheet
    if (showCitySheet) {
        CitySelectionSheet(
            selectedCity = selectedCity,
            onCitySelected = {
                onCitySelected(it)
                showCitySheet = false
            },
            onUseGpsLocation = onUseGpsLocation,
            onDismiss = { showCitySheet = false }
        )
    }
}
