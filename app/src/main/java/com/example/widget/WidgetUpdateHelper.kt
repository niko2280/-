package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetUpdateHelper {

    fun updateWidget(
        context: Context,
        cityName: String,
        temperature: Int,
        feelsLike: Int,
        weatherCode: Int,
        condition: String,
        rainStatus: String,
        minTemp: Int,
        maxTemp: Int
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

        // Text data
        views.setTextViewText(R.id.widget_city, cityName)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.widget_update_time, timeStr)

        val sign = if (temperature > 0) "+" else ""
        views.setTextViewText(R.id.widget_temp, "$sign$temperature°")

        val feelsSign = if (feelsLike > 0) "+" else ""
        views.setTextViewText(R.id.widget_feels_like, "Ощущ. $feelsSign$feelsLike°")

        views.setTextViewText(R.id.widget_condition, condition)

        val rainDisplay = if (rainStatus.isNotBlank()) "🌧️ $rainStatus" else "Осадков нет"
        views.setTextViewText(R.id.widget_rain_status, rainDisplay)

        views.setTextViewText(R.id.widget_temp_range, "макс +$maxTemp° • мин +$minTemp°")

        // PendingIntent on container to launch app
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, launchPendingIntent)

        // PendingIntent for refresh button
        val refreshIntent = Intent(context, WeatherWidgetProvider::class.java).apply {
            action = WeatherWidgetProvider.ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

        // Update all active widgets
        appWidgetManager.updateAppWidget(widgetIds, views)
    }
}
