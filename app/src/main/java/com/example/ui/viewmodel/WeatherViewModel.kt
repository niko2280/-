package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BelarusCity
import com.example.data.model.FullWeatherState
import com.example.data.model.RadarTimelineSnapshot
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.hypot

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _selectedCity = MutableStateFlow(BelarusCity.DEFAULT)
    val selectedCity: StateFlow<BelarusCity> = _selectedCity.asStateFlow()

    private val _weatherState = MutableStateFlow<FullWeatherState?>(null)
    val weatherState: StateFlow<FullWeatherState?> = _weatherState.asStateFlow()

    private val _radarFrames = MutableStateFlow<List<RadarTimelineSnapshot>>(emptyList())
    val radarFrames: StateFlow<List<RadarTimelineSnapshot>> = _radarFrames.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Погода, 1: Радар Беларуси
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    init {
        loadWeather(BelarusCity.DEFAULT)
    }

    fun setActiveTab(index: Int) {
        _activeTab.value = index
    }

    fun selectCity(city: BelarusCity) {
        _selectedCity.value = city
        loadWeather(city)
    }

    fun refresh() {
        loadWeather(_selectedCity.value)
    }

    private fun loadWeather(city: BelarusCity) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getFullWeather(city)
            result.onSuccess { state ->
                _weatherState.value = state
                val frames = repository.getRadarTimelineFrames(city, state.precipitationNowMmH)
                _radarFrames.value = frames
            }.onFailure {
                // Keep existing or fallback
            }
            _isLoading.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGpsLocation(context: Context) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    // Find closest Belarusian city
                    val nearest = BelarusCity.ALL.minByOrNull { city ->
                        hypot(city.latitude - lat, city.longitude - lon)
                    }
                    if (nearest != null) {
                        selectCity(nearest)
                    }
                }
            }
        } catch (e: Exception) {
            // Gracefully ignore permission denied or missing provider
        }
    }
}
