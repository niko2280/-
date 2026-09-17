package com.example.data.model

data class RadarCell(
    val id: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Float,
    val intensityMmH: Float,
    val cloudDensity: Float,
    val precipitationType: PrecipitationType,
    val velocityKmH: Float = 35f,
    val directionDeg: Float = 60f // typical SW to NE movement in Belarus
)

data class RadarTimelineSnapshot(
    val minuteOffset: Int, // e.g. -30, -15, 0 (NOW), 15, 30, 45, 60, 75, 90, 105, 120
    val label: String,
    val cells: List<RadarCell>,
    val regionalPrecipitation: Map<String, Float> // "minsk" -> 1.8f
)

data class MapLocationInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val precipitationMmH: Float,
    val cloudCoveragePct: Int,
    val statusText: String,
    val forecastText: String
)

data class BelarusBoundary(
    val minLat: Double = 51.2,
    val maxLat: Double = 56.2,
    val minLon: Double = 23.1,
    val maxLon: Double = 32.8
) {
    fun toNormalized(lat: Double, lon: Double): Pair<Float, Float> {
        val x = ((lon - minLon) / (maxLon - minLon)).toFloat().coerceIn(0f, 1f)
        // In screen coords, Y goes top (high lat) to bottom (low lat)
        val y = ((maxLat - lat) / (maxLat - minLat)).toFloat().coerceIn(0f, 1f)
        return Pair(x, y)
    }
}
