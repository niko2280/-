package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.WeatherApplication
import com.example.data.model.BelarusCity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshWeather(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshWeather(context)
        }
    }

    private fun refreshWeather(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? WeatherApplication
                val repository = app?.weatherRepository
                if (repository != null) {
                    // Update default or first city
                    repository.getFullWeather(BelarusCity.DEFAULT)
                }
            } catch (e: Exception) {
                // Ignore background errors
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.aistudio.weatherby.ACTION_WIDGET_REFRESH"
    }
}
