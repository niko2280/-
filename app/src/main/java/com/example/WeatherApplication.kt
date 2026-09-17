package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.api.WeatherApiService
import com.example.data.local.WeatherDatabase
import com.example.data.repository.WeatherRepository

class WeatherApplication : Application() {

    lateinit var database: WeatherDatabase
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            WeatherDatabase::class.java,
            "belarus_weather.db"
        ).fallbackToDestructiveMigration().build()

        val apiService = WeatherApiService.create()
        weatherRepository = WeatherRepository(
            apiService = apiService,
            weatherDao = database.weatherDao(),
            context = applicationContext
        )
    }

    companion object {
        lateinit var instance: WeatherApplication
            private set
    }
}
