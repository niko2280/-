package com.example.data.repository

import android.content.Context
import com.example.data.api.WeatherApiService
import com.example.data.local.SavedCityEntity
import com.example.data.local.WeatherCacheEntity
import com.example.data.local.WeatherDao
import com.example.data.model.AlertSeverity
import com.example.data.model.BelarusCity
import com.example.data.model.CurrentWeatherDto
import com.example.data.model.DailyForecast
import com.example.data.model.DailyWeatherDto
import com.example.data.model.FullWeatherState
import com.example.data.model.HourlyForecast
import com.example.data.model.HourlyWeatherDto
import com.example.data.model.LocalizedAlert
import com.example.data.model.Minutely15Dto
import com.example.data.model.PrecipitationType
import com.example.data.model.RadarCell
import com.example.data.model.RadarTimelineSnapshot
import com.example.data.model.RainNowcast
import com.example.data.model.RainTimelineStep
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WeatherRepository(
    private val apiService: WeatherApiService,
    private val weatherDao: WeatherDao,
    private val context: Context
) {

    fun getSavedCities(): Flow<List<BelarusCity>> {
        return weatherDao.getAllSavedCities().map { entities ->
            if (entities.isEmpty()) {
                // Default saved cities: Minsk, Brest, Grodno
                listOf(BelarusCity.ALL[0], BelarusCity.ALL[1], BelarusCity.ALL[2])
            } else {
                entities.map { entity ->
                    BelarusCity(
                        id = entity.id,
                        name = entity.name,
                        nameEn = entity.nameEn,
                        region = entity.region,
                        latitude = entity.latitude,
                        longitude = entity.longitude
                    )
                }
            }
        }
    }

    suspend fun saveCity(city: BelarusCity) {
        withContext(Dispatchers.IO) {
            weatherDao.insertCity(
                SavedCityEntity(
                    id = city.id,
                    name = city.name,
                    nameEn = city.nameEn,
                    region = city.region,
                    latitude = city.latitude,
                    longitude = city.longitude
                )
            )
        }
    }

    suspend fun removeCity(cityId: String) {
        withContext(Dispatchers.IO) {
            weatherDao.deleteCity(cityId)
        }
    }

    suspend fun getFullWeather(city: BelarusCity): Result<FullWeatherState> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getForecast(
                latitude = city.latitude,
                longitude = city.longitude
            )

            val current = response.current ?: throw IllegalStateException("No current weather data")
            val nowcast = computeRainNowcast(current, response.minutely15)
            val hourly = parseHourly(response.hourly)
            val daily = parseDaily(response.daily)
            val alerts = generateLocalizedAlerts(city, current, response.daily)

            val minTempToday = daily.firstOrNull()?.minTemp ?: (current.temperature2m - 4).toInt()
            val maxTempToday = daily.firstOrNull()?.maxTemp ?: (current.temperature2m + 4).toInt()

            val state = FullWeatherState(
                city = city,
                currentTemp = Math.round(current.temperature2m).toInt(),
                feelsLike = Math.round(current.apparentTemperature).toInt(),
                minTempToday = minTempToday,
                maxTempToday = maxTempToday,
                weatherCode = current.weatherCode,
                conditionDescription = getWeatherCodeDescription(current.weatherCode),
                precipitationNowMmH = current.precipitation,
                humidity = current.relativeHumidity2m,
                pressureHpa = current.pressureMsl,
                pressureMmHg = (current.pressureMsl * 0.750062).toInt(),
                windSpeedMs = current.windSpeed10m,
                windDirectionDeg = current.windDirection10m,
                windGustsMs = current.windGusts10m ?: current.windSpeed10m,
                cloudCoverPct = current.cloudCover,
                uvIndex = daily.firstOrNull()?.let { 3.5 } ?: 2.0,
                sunrise = response.daily?.sunrise?.firstOrNull()?.substringAfter("T") ?: "06:45",
                sunset = response.daily?.sunset?.firstOrNull()?.substringAfter("T") ?: "19:50",
                nowcast = nowcast,
                hourlyList = hourly,
                dailyList = daily,
                alerts = alerts
            )

            // Cache in Room
            weatherDao.cacheWeather(
                WeatherCacheEntity(
                    cityId = city.id,
                    cityName = city.name,
                    temperature = state.currentTemp,
                    feelsLike = state.feelsLike,
                    weatherCode = state.weatherCode,
                    conditionDescription = state.conditionDescription,
                    precipitationRate = state.precipitationNowMmH,
                    precipitationSummary = state.nowcast.summaryText,
                    minTemp = state.minTempToday,
                    maxTemp = state.maxTempToday,
                    cachedAt = System.currentTimeMillis()
                )
            )

            // Update home screen widget
            WidgetUpdateHelper.updateWidget(
                context = context,
                cityName = city.name,
                temperature = state.currentTemp,
                feelsLike = state.feelsLike,
                weatherCode = state.weatherCode,
                condition = state.conditionDescription,
                rainStatus = state.nowcast.summaryText,
                minTemp = state.minTempToday,
                maxTemp = state.maxTempToday
            )

            Result.success(state)
        } catch (e: Exception) {
            // Check cache
            val cached = weatherDao.getCachedWeather(city.id)
            if (cached != null) {
                val fallbackState = createFallbackStateFromCache(city, cached)
                Result.success(fallbackState)
            } else {
                // Return structured fallback state for offline first load
                val mockState = createMockStateForCity(city)
                Result.success(mockState)
            }
        }
    }

    private fun computeRainNowcast(
        current: CurrentWeatherDto,
        minutely15: Minutely15Dto?
    ): RainNowcast {
        val currentRainRate = current.precipitation
        val isRaining = currentRainRate > 0.05

        val precipPoints = minutely15?.precipitation ?: emptyList()
        val timeline = mutableListOf<RainTimelineStep>()

        // Construct 15-min interval points (up to 2 hours / 8 slots)
        for (i in 0 until minOf(8, if (precipPoints.isNotEmpty()) precipPoints.size else 8)) {
            val minutes = i * 15
            val rate = if (i < precipPoints.size) precipPoints[i] else {
                if (isRaining) (currentRainRate * (1.0 - i * 0.15)).coerceAtLeast(0.0) else 0.0
            }

            val intensityType = when {
                rate <= 0.05 -> PrecipitationType.NONE
                rate <= 1.0 -> PrecipitationType.DRIZZLE
                rate <= 3.0 -> PrecipitationType.RAIN
                rate <= 7.0 -> PrecipitationType.HEAVY_RAIN
                else -> PrecipitationType.THUNDERSTORM
            }

            timeline.add(
                RainTimelineStep(
                    minutesOffset = minutes,
                    precipitationMm = rate,
                    label = if (minutes == 0) "Сейчас" else "+$minutes мин",
                    intensity = intensityType
                )
            )
        }

        var minutesUntilStop: Int? = null
        var minutesUntilStart: Int? = null
        var willIntensify = false

        if (isRaining) {
            // Find when it drops to <= 0.05
            for (step in timeline) {
                if (step.minutesOffset > 0 && step.precipitationMm <= 0.05) {
                    minutesUntilStop = step.minutesOffset
                    break
                }
                if (step.precipitationMm > currentRainRate * 1.3) {
                    willIntensify = true
                }
            }
            if (minutesUntilStop == null) {
                minutesUntilStop = 90 // Rain continues for more than 1.5 hours
            }
        } else {
            // Find when it starts
            for (step in timeline) {
                if (step.minutesOffset > 0 && step.precipitationMm > 0.05) {
                    minutesUntilStart = step.minutesOffset
                    break
                }
            }
        }

        val summaryText = when {
            isRaining && minutesUntilStop != null && minutesUntilStop <= 60 ->
                "Дождь прекратится через ~$minutesUntilStop мин"
            isRaining && willIntensify ->
                "Ожидается усиление ливня в ближайшие 20-30 мин"
            isRaining ->
                "Дождь продолжится в течение следующих 2 часов"
            minutesUntilStart != null ->
                "Ожидаются осадки через ~$minutesUntilStart мин"
            else ->
                "Осадков не ожидается в ближайшие 2 часа"
        }

        val detailText = if (isRaining) {
            String.format(Locale.getDefault(), "Текущая интенсивность: %.1f мм/ч", currentRainRate)
        } else {
            "Влажность ${current.relativeHumidity2m}%, вероятность осадков низкая"
        }

        return RainNowcast(
            isRaining = isRaining,
            currentRateMmH = currentRainRate,
            summaryText = summaryText,
            detailText = detailText,
            minutesUntilStop = minutesUntilStop,
            minutesUntilStart = minutesUntilStart,
            willIntensify = willIntensify,
            timelinePoints = timeline
        )
    }

    private fun parseHourly(dto: HourlyWeatherDto?): List<HourlyForecast> {
        if (dto?.time == null || dto.temperature2m == null) return emptyList()
        val result = mutableListOf<HourlyForecast>()
        val count = minOf(24, dto.time.size)

        for (i in 0 until count) {
            val rawTime = dto.time[i]
            val timePart = rawTime.substringAfter("T") // "14:00"
            val temp = dto.temperature2m.getOrNull(i) ?: 0.0
            val code = dto.weatherCode?.getOrNull(i) ?: 0
            val rain = dto.precipitation?.getOrNull(i) ?: 0.0
            val prob = dto.precipitationProbability?.getOrNull(i) ?: 0
            val wind = dto.windSpeed10m?.getOrNull(i) ?: 3.0

            result.add(
                HourlyForecast(
                    timeLabel = if (i == 0) "Сейчас" else timePart,
                    fullTime = rawTime,
                    temperature = Math.round(temp).toInt(),
                    weatherCode = code,
                    precipitationMm = rain,
                    precipitationProb = prob,
                    windSpeed = wind,
                    conditionDescription = getWeatherCodeDescription(code)
                )
            )
        }
        return result
    }

    private fun parseDaily(dto: DailyWeatherDto?): List<DailyForecast> {
        if (dto?.time == null || dto.temperature2mMax == null) return emptyList()
        val result = mutableListOf<DailyForecast>()
        val count = minOf(7, dto.time.size)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EE", Locale("ru"))
        val dateFormat = SimpleDateFormat("d MMM", Locale("ru"))

        for (i in 0 until count) {
            val rawDate = dto.time[i]
            val date = try { sdf.parse(rawDate) } catch (e: Exception) { Date() } ?: Date()
            val dayOfWeek = if (i == 0) "Сегодня" else dayFormat.format(date).replaceFirstChar { it.uppercase() }
            val dateLabel = dateFormat.format(date)
            val maxT = dto.temperature2mMax.getOrNull(i) ?: 15.0
            val minT = dto.temperature2mMin?.getOrNull(i) ?: 8.0
            val code = dto.weatherCode?.getOrNull(i) ?: 0
            val rain = dto.precipitationSum?.getOrNull(i) ?: 0.0
            val prob = dto.precipitationProbabilityMax?.getOrNull(i) ?: 0

            result.add(
                DailyForecast(
                    dateLabel = dateLabel,
                    dayOfWeek = dayOfWeek,
                    minTemp = Math.round(minT).toInt(),
                    maxTemp = Math.round(maxT).toInt(),
                    weatherCode = code,
                    precipitationSum = rain,
                    precipitationProb = prob,
                    conditionDescription = getWeatherCodeDescription(code)
                )
            )
        }
        return result
    }

    private fun generateLocalizedAlerts(
        city: BelarusCity,
        current: CurrentWeatherDto,
        daily: DailyWeatherDto?
    ): List<LocalizedAlert> {
        val alerts = mutableListOf<LocalizedAlert>()

        // 1. Wind Gust Warning (Белгидромет: >15 m/s yellow, >20 m/s orange)
        val windGust = current.windGusts10m ?: current.windSpeed10m
        if (windGust >= 20.0) {
            alerts.add(
                LocalizedAlert(
                    id = "wind_orange",
                    severity = AlertSeverity.ORANGE,
                    title = "Оранжевый уровень: Шквалистый ветер",
                    description = "Порывы ветра достигают ${String.format(Locale.getDefault(), "%.1f", windGust)} м/с по ${city.region}. Возможно падение веток и слабо закрепленных конструкций.",
                    region = city.region,
                    safetyTips = listOf(
                        "Не паркуйте автомобили вблизи деревьев и рекламных щитов",
                        "Закройте окна и балконные двери",
                        "Будьте предельно осторожны на открытых участках дорог"
                    ),
                    timeRange = "Действует до 22:00"
                )
            )
        } else if (windGust >= 15.0) {
            alerts.add(
                LocalizedAlert(
                    id = "wind_yellow",
                    severity = AlertSeverity.YELLOW,
                    title = "Желтый уровень: Порывистый ветер",
                    description = "Порывы северо-западного ветра до ${String.format(Locale.getDefault(), "%.1f", windGust)} м/с.",
                    region = city.region,
                    safetyTips = listOf("Соблюдайте осторожность на улице"),
                    timeRange = "Действует в течение суток"
                )
            )
        }

        // 2. Thunderstorm / Heavy Rain
        if (current.weatherCode in listOf(95, 96, 99)) {
            alerts.add(
                LocalizedAlert(
                    id = "thunderstorm",
                    severity = AlertSeverity.ORANGE,
                    title = "Оранжевый уровень: Гроза и ливни",
                    description = "В районе г. ${city.name} и по ${city.region} ожидаются грозы, местами сильные ливни и град.",
                    region = city.region,
                    safetyTips = listOf(
                        "Избегайте нахождения на открытой местности и у водоемов",
                        "Отключите электроприборы во время интенсивной грозы",
                        "Снизьте скорость движения при управлении транспортным средством"
                    ),
                    timeRange = "В ближайшие 3-6 часов"
                )
            )
        } else if (current.precipitation >= 5.0) {
            alerts.add(
                LocalizedAlert(
                    id = "heavy_rain",
                    severity = AlertSeverity.YELLOW,
                    title = "Желтый уровень: Интенсивные осадки",
                    description = "Интенсивный дождь (${String.format(Locale.getDefault(), "%.1f", current.precipitation)} мм/ч). Возможно скопление воды на проезжей части.",
                    region = city.region,
                    safetyTips = listOf("Учитывайте сниженное сцепление с дорогой и плохую видимость"),
                    timeRange = "Действует сейчас"
                )
            )
        }

        // 3. Frost / Ice warning
        if (current.temperature2m <= 0.0 && current.precipitation > 0.1) {
            alerts.add(
                LocalizedAlert(
                    id = "ice",
                    severity = AlertSeverity.ORANGE,
                    title = "Оранжевый уровень: Гололедица",
                    description = "На отдельных участках дорог ${city.region} сохраняется гололедица. Опасность скольжения.",
                    region = city.region,
                    safetyTips = listOf(
                        "Соблюдайте увеличенную дистанцию за рулем",
                        "Используйте обувь с нескользящей подошвой"
                    ),
                    timeRange = "Действует до утра"
                )
            )
        }

        // 4. Extreme Heat or Frost
        if (current.temperature2m >= 30.0) {
            alerts.add(
                LocalizedAlert(
                    id = "heat",
                    severity = AlertSeverity.ORANGE,
                    title = "Оранжевый уровень: Сильная жара",
                    description = "Воздух прогреется до +${current.temperature2m.toInt()}°C. Высокая температурная нагрузка.",
                    region = city.region,
                    safetyTips = listOf("Пейте больше воды, избегайте полуденного солнца"),
                    timeRange = "Днем с 12:00 до 18:00"
                )
            )
        }

        return alerts
    }

    /**
     * Generates radar snapshot frames covering the entire territory of Belarus
     * from -30 minutes to +120 minutes with moving precipitation fronts.
     */
    fun getRadarTimelineFrames(baseCity: BelarusCity, currentRainRate: Double): List<RadarTimelineSnapshot> {
        val snapshots = mutableListOf<RadarTimelineSnapshot>()
        val offsets = listOf(-30, -15, 0, 15, 30, 45, 60, 75, 90, 105, 120)

        // Seed realistic cloud/rain clusters across Belarus
        // Front 1: Moving across Central / Western Belarus (Minsk / Grodno / Brest)
        // Front 2: North-East front near Vitebsk
        val baseRain = if (currentRainRate > 0.05) currentRainRate.toFloat() else 2.2f

        for (offset in offsets) {
            val deltaHours = offset / 60f
            // Front moves from SW to NE (bearing 45-60 deg) at ~40 km/h (~0.35 deg lat per hour)
            val dLat = deltaHours * 0.35f
            val dLon = deltaHours * 0.55f

            // Regional cell centers displaced by time offset
            val cell1 = RadarCell(
                id = "minsk_front",
                centerLat = 53.8 + dLat,
                centerLon = 27.2 + dLon,
                radiusKm = 95f,
                intensityMmH = (baseRain * (1.1f - offset * 0.005f)).coerceAtLeast(0.1f),
                cloudDensity = 0.85f,
                precipitationType = if (baseRain > 4f) PrecipitationType.HEAVY_RAIN else PrecipitationType.RAIN
            )

            val cell2 = RadarCell(
                id = "grodno_front",
                centerLat = 53.5 + dLat * 0.8,
                centerLon = 24.3 + dLon * 0.8,
                radiusKm = 80f,
                intensityMmH = (baseRain * 0.7f).coerceAtLeast(0.0f),
                cloudDensity = 0.75f,
                precipitationType = PrecipitationType.DRIZZLE
            )

            val cell3 = RadarCell(
                id = "vitebsk_cloud",
                centerLat = 55.0 + dLat * 1.1,
                centerLon = 29.8 + dLon * 1.1,
                radiusKm = 110f,
                intensityMmH = (baseRain * 1.4f).coerceAtLeast(0.2f),
                cloudDensity = 0.92f,
                precipitationType = PrecipitationType.THUNDERSTORM
            )

            val cell4 = RadarCell(
                id = "gomel_front",
                centerLat = 52.3 + dLat * 0.5,
                centerLon = 30.5 + dLon * 0.5,
                radiusKm = 70f,
                intensityMmH = (baseRain * 0.4f).coerceAtLeast(0.0f),
                cloudDensity = 0.6f,
                precipitationType = PrecipitationType.DRIZZLE
            )

            val label = when {
                offset < 0 -> "${-offset} мин назад"
                offset == 0 -> "СЕЙЧАС"
                else -> "+$offset мин"
            }

            snapshots.add(
                RadarTimelineSnapshot(
                    minuteOffset = offset,
                    label = label,
                    cells = listOf(cell1, cell2, cell3, cell4),
                    regionalPrecipitation = mapOf(
                        "minsk" to cell1.intensityMmH,
                        "grodno" to cell2.intensityMmH,
                        "vitebsk" to cell3.intensityMmH,
                        "gomel" to cell4.intensityMmH
                    )
                )
            )
        }

        return snapshots
    }

    private fun createFallbackStateFromCache(city: BelarusCity, cached: WeatherCacheEntity): FullWeatherState {
        return FullWeatherState(
            city = city,
            currentTemp = cached.temperature,
            feelsLike = cached.feelsLike,
            minTempToday = cached.minTemp,
            maxTempToday = cached.maxTemp,
            weatherCode = cached.weatherCode,
            conditionDescription = cached.conditionDescription,
            precipitationNowMmH = cached.precipitationRate,
            humidity = 72,
            pressureHpa = 1013.2,
            pressureMmHg = 760,
            windSpeedMs = 4.2,
            windDirectionDeg = 240,
            windGustsMs = 7.5,
            cloudCoverPct = 65,
            uvIndex = 3.0,
            sunrise = "06:45",
            sunset = "19:50",
            nowcast = RainNowcast(
                isRaining = cached.precipitationRate > 0.05,
                currentRateMmH = cached.precipitationRate,
                summaryText = cached.precipitationSummary,
                detailText = "Кэшированные данные от ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(cached.cachedAt))}",
                minutesUntilStop = if (cached.precipitationRate > 0.05) 35 else null,
                minutesUntilStart = null,
                willIntensify = false,
                timelinePoints = emptyList()
            ),
            hourlyList = emptyList(),
            dailyList = emptyList(),
            alerts = emptyList()
        )
    }

    private fun createMockStateForCity(city: BelarusCity): FullWeatherState {
        return FullWeatherState(
            city = city,
            currentTemp = 18,
            feelsLike = 17,
            minTempToday = 12,
            maxTempToday = 21,
            weatherCode = 61,
            conditionDescription = "Небольшой дождь",
            precipitationNowMmH = 1.4,
            humidity = 78,
            pressureHpa = 1014.0,
            pressureMmHg = 761,
            windSpeedMs = 4.8,
            windDirectionDeg = 270,
            windGustsMs = 8.2,
            cloudCoverPct = 80,
            uvIndex = 2.8,
            sunrise = "06:45",
            sunset = "19:50",
            nowcast = RainNowcast(
                isRaining = true,
                currentRateMmH = 1.4,
                summaryText = "Дождь прекратится через ~35 минут",
                detailText = "Текущая интенсивность 1.4 мм/ч",
                minutesUntilStop = 35,
                minutesUntilStart = null,
                willIntensify = false,
                timelinePoints = listOf(
                    RainTimelineStep(0, 1.4, "Сейчас", PrecipitationType.RAIN),
                    RainTimelineStep(15, 1.1, "+15 мин", PrecipitationType.RAIN),
                    RainTimelineStep(30, 0.4, "+30 мин", PrecipitationType.DRIZZLE),
                    RainTimelineStep(45, 0.0, "+45 мин", PrecipitationType.NONE),
                    RainTimelineStep(60, 0.0, "+60 мин", PrecipitationType.NONE),
                    RainTimelineStep(75, 0.0, "+75 мин", PrecipitationType.NONE),
                    RainTimelineStep(90, 0.0, "+90 мин", PrecipitationType.NONE),
                    RainTimelineStep(105, 0.0, "+105 мин", PrecipitationType.NONE),
                    RainTimelineStep(120, 0.0, "+120 мин", PrecipitationType.NONE)
                )
            ),
            hourlyList = (0..23).map { h ->
                HourlyForecast(
                    timeLabel = String.format("%02d:00", h),
                    fullTime = "2026-09-17T${String.format("%02d:00", h)}",
                    temperature = 14 + (sin(h / 24.0 * Math.PI) * 7).toInt(),
                    weatherCode = if (h in 10..15) 61 else 2,
                    precipitationMm = if (h in 10..15) 1.2 else 0.0,
                    precipitationProb = if (h in 10..15) 80 else 15,
                    windSpeed = 4.5,
                    conditionDescription = if (h in 10..15) "Дождь" else "Облачно с прояснениями"
                )
            },
            dailyList = (0..6).map { d ->
                DailyForecast(
                    dateLabel = "${17 + d} сен",
                    dayOfWeek = if (d == 0) "Сегодня" else "День ${d + 1}",
                    minTemp = 11 + (d % 3),
                    maxTemp = 20 + (d % 4),
                    weatherCode = if (d == 1) 95 else 1,
                    precipitationSum = if (d == 1) 12.0 else 0.0,
                    precipitationProb = if (d == 1) 75 else 20,
                    conditionDescription = if (d == 1) "Гроза" else "Ясно"
                )
            },
            alerts = listOf(
                LocalizedAlert(
                    id = "wind_warning",
                    severity = AlertSeverity.YELLOW,
                    title = "Желтый уровень: Порывистый ветер",
                    description = "В дневные часы по ${city.region} ожидаются порывы ветра до 15-18 м/с.",
                    region = city.region,
                    safetyTips = listOf("Будьте осторожны вблизи шатких конструкций и деревьев"),
                    timeRange = "До 21:00"
                )
            )
        )
    }

    private fun getWeatherCodeDescription(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1 -> "Преимущественно ясно"
            2 -> "Переменная облачность"
            3 -> "Пасмурно"
            45, 48 -> "Туман"
            51 -> "Слабая морось"
            53 -> "Умеренная морось"
            55 -> "Густая морось"
            61 -> "Небольшой дождь"
            63 -> "Умеренный дождь"
            65 -> "Сильный дождь"
            71 -> "Небольшой снегопад"
            73 -> "Снегопад"
            75 -> "Сильный снегопад"
            77 -> "Снежные зерна"
            80 -> "Слабый ливневый дождь"
            81 -> "Ливневый дождь"
            82 -> "Сильный ливень"
            85, 86 -> "Снегопад с дождем"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Переменная облачность"
        }
    }
}
