package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "timezone") val timezone: String,
    @Json(name = "current") val current: CurrentWeatherDto?,
    @Json(name = "minutely_15") val minutely15: Minutely15Dto?,
    @Json(name = "hourly") val hourly: HourlyWeatherDto?,
    @Json(name = "daily") val daily: DailyWeatherDto?
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "precipitation") val precipitation: Double,
    @Json(name = "rain") val rain: Double?,
    @Json(name = "showers") val showers: Double?,
    @Json(name = "snowfall") val snowfall: Double?,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "cloud_cover") val cloudCover: Int,
    @Json(name = "pressure_msl") val pressureMsl: Double,
    @Json(name = "surface_pressure") val surfacePressure: Double?,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Int,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double?
)

@JsonClass(generateAdapter = true)
data class Minutely15Dto(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "precipitation") val precipitation: List<Double>?,
    @Json(name = "rain") val rain: List<Double>?,
    @Json(name = "snowfall") val snowfall: List<Double>?
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>?,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @Json(name = "precipitation") val precipitation: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "cloud_cover") val cloudCover: List<Int>?,
    @Json(name = "pressure_msl") val pressureMsl: List<Double>?,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double>?,
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int>?,
    @Json(name = "uv_index") val uvIndex: List<Double>?
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double>?,
    @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double>?,
    @Json(name = "sunrise") val sunrise: List<String>?,
    @Json(name = "sunset") val sunset: List<String>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>?
)

// UI Domain Models

enum class PrecipitationType {
    NONE, DRIZZLE, RAIN, HEAVY_RAIN, THUNDERSTORM, SNOW, SLEET
}

data class RainNowcast(
    val isRaining: Boolean,
    val currentRateMmH: Double,
    val summaryText: String,
    val detailText: String,
    val minutesUntilStop: Int?,
    val minutesUntilStart: Int?,
    val willIntensify: Boolean,
    val timelinePoints: List<RainTimelineStep>
)

data class RainTimelineStep(
    val minutesOffset: Int,
    val precipitationMm: Double,
    val label: String,
    val intensity: PrecipitationType
)

data class HourlyForecast(
    val timeLabel: String,
    val fullTime: String,
    val temperature: Int,
    val weatherCode: Int,
    val precipitationMm: Double,
    val precipitationProb: Int,
    val windSpeed: Double,
    val conditionDescription: String
)

data class DailyForecast(
    val dateLabel: String,
    val dayOfWeek: String,
    val minTemp: Int,
    val maxTemp: Int,
    val weatherCode: Int,
    val precipitationSum: Double,
    val precipitationProb: Int,
    val conditionDescription: String
)

enum class AlertSeverity {
    YELLOW, ORANGE, RED
}

data class LocalizedAlert(
    val id: String,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val region: String,
    val safetyTips: List<String>,
    val timeRange: String
)

data class FullWeatherState(
    val city: BelarusCity,
    val currentTemp: Int,
    val feelsLike: Int,
    val minTempToday: Int,
    val maxTempToday: Int,
    val weatherCode: Int,
    val conditionDescription: String,
    val precipitationNowMmH: Double,
    val humidity: Int,
    val pressureHpa: Double,
    val pressureMmHg: Int,
    val windSpeedMs: Double,
    val windDirectionDeg: Int,
    val windGustsMs: Double,
    val cloudCoverPct: Int,
    val uvIndex: Double,
    val sunrise: String,
    val sunset: String,
    val nowcast: RainNowcast,
    val hourlyList: List<HourlyForecast>,
    val dailyList: List<DailyForecast>,
    val alerts: List<LocalizedAlert>,
    val lastUpdated: Long = System.currentTimeMillis()
)
