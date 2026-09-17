package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_cities")
data class SavedCityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameEn: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val cityId: String,
    val cityName: String,
    val temperature: Int,
    val feelsLike: Int,
    val weatherCode: Int,
    val conditionDescription: String,
    val precipitationRate: Double,
    val precipitationSummary: String,
    val minTemp: Int,
    val maxTemp: Int,
    val cachedAt: Long
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM saved_cities ORDER BY isFavorite DESC, addedAt ASC")
    fun getAllSavedCities(): Flow<List<SavedCityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: SavedCityEntity)

    @Query("DELETE FROM saved_cities WHERE id = :cityId")
    suspend fun deleteCity(cityId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheWeather(cache: WeatherCacheEntity)

    @Query("SELECT * FROM weather_cache WHERE cityId = :cityId LIMIT 1")
    suspend fun getCachedWeather(cityId: String): WeatherCacheEntity?
}

@Database(
    entities = [SavedCityEntity::class, WeatherCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
