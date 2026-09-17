package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.RadarMapScreen
import com.example.ui.screens.WeatherHomeScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val app = context.applicationContext as WeatherApplication

                val viewModel: WeatherViewModel = viewModel {
                    WeatherViewModel(app.weatherRepository)
                }

                val activeTab by viewModel.activeTab.collectAsState()
                val selectedCity by viewModel.selectedCity.collectAsState()
                val weatherState by viewModel.weatherState.collectAsState()
                val radarFrames by viewModel.radarFrames.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                // Runtime GPS permission launcher
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (fineGranted || coarseGranted) {
                        viewModel.requestGpsLocation(context)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation_bar"),
                            containerColor = DarkSurface,
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = activeTab == 0,
                                onClick = { viewModel.setActiveTab(0) },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == 0) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                                        contentDescription = "Погода"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Погода",
                                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00354E),
                                    selectedTextColor = SkyBluePrimary,
                                    indicatorColor = SkyBluePrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("weather_tab")
                            )

                            NavigationBarItem(
                                selected = activeTab == 1,
                                onClick = { viewModel.setActiveTab(1) },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == 1) Icons.Filled.Map else Icons.Outlined.Map,
                                        contentDescription = "Радар Беларуси"
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Радар Беларуси",
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00354E),
                                    selectedTextColor = SkyBluePrimary,
                                    indicatorColor = SkyBluePrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("radar_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition",
                        modifier = Modifier.padding(innerPadding)
                    ) { tab ->
                        when (tab) {
                            0 -> WeatherHomeScreen(
                                weatherState = weatherState,
                                isLoading = isLoading,
                                selectedCity = selectedCity,
                                onCitySelected = { viewModel.selectCity(it) },
                                onRefresh = { viewModel.refresh() },
                                onUseGpsLocation = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onOpenRadar = { viewModel.setActiveTab(1) }
                            )
                            1 -> {
                                val state = weatherState
                                if (state != null) {
                                    RadarMapScreen(
                                        weatherState = state,
                                        radarFrames = radarFrames,
                                        selectedCity = selectedCity,
                                        onCitySelected = { viewModel.selectCity(it) }
                                    )
                                } else {
                                    WeatherHomeScreen(
                                        weatherState = null,
                                        isLoading = isLoading,
                                        selectedCity = selectedCity,
                                        onCitySelected = { viewModel.selectCity(it) },
                                        onRefresh = { viewModel.refresh() },
                                        onUseGpsLocation = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        onOpenRadar = { viewModel.setActiveTab(1) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
