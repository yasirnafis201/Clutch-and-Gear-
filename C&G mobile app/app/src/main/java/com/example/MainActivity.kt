package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppHeader
import com.example.ui.components.AppScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.ObdConnectScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TelemetryScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.theme.ClutchAndGearTheme
import com.example.ui.theme.CngGraphiteDarkest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClutchAndGearTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val subScreen by viewModel.currentSubScreen.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()

    val allVehicles by viewModel.allVehicles.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val recentSamples by viewModel.recentSamples.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val latestDtcScan by viewModel.latestDtcScan.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val maintenanceRecords by viewModel.maintenanceRecords.collectAsState()
    val aiMessages by viewModel.aiMessages.collectAsState()
    val isScanningDtc by viewModel.isScanningDtc.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val currentTransport by viewModel.currentTransport.collectAsState()
    val drivingProfile by viewModel.drivingProfile.collectAsState()

    // Permission launcher for Bluetooth & Audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle result
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Handle back button for sub-screens or secondary tabs
    BackHandler(enabled = subScreen != null || currentScreen != AppScreen.DASHBOARD) {
        if (subScreen != null) {
            viewModel.currentSubScreen.value = null
        } else {
            viewModel.currentScreen.value = AppScreen.DASHBOARD
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            onGetStarted = { viewModel.showOnboarding.value = false },
            onStartDemoMode = { viewModel.showOnboarding.value = false }
        )
        return
    }

    Scaffold(
        topBar = {
            AppHeader(
                currentVehicle = selectedVehicle,
                allVehicles = allVehicles,
                connectionState = connectionState,
                onSelectVehicle = { id -> viewModel.selectVehicle(id) },
                onConnectClick = { viewModel.currentSubScreen.value = SubScreen.OBD_CONNECT },
                onSettingsClick = { viewModel.currentSubScreen.value = SubScreen.SETTINGS }
            )
        },
        bottomBar = {
            if (subScreen == null) {
                AppBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen -> viewModel.currentScreen.value = screen }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CngGraphiteDarkest)
        ) {
            if (subScreen != null) {
                when (subScreen) {
                    SubScreen.TRIPS -> TripsScreen(trips = trips)
                    SubScreen.MAINTENANCE -> MaintenanceScreen(
                        vehicle = selectedVehicle,
                        records = maintenanceRecords,
                        onAddRecord = { odo, cat, work, parts, cost, nextKm, notes ->
                            viewModel.addMaintenanceRecord(odo, cat, work, parts, cost, nextKm, notes)
                        }
                    )
                    SubScreen.OBD_CONNECT -> ObdConnectScreen(
                        currentTransport = currentTransport,
                        connectionState = connectionState,
                        activeDevice = null,
                        availableDevices = availableDevices,
                        onSelectTransport = { trans -> viewModel.selectTransport(trans) },
                        onDiscoverDevices = { viewModel.discoverDevices() },
                        onConnectDevice = { dev -> viewModel.connectDevice(dev) },
                        onDisconnect = { viewModel.disconnectAdapter() }
                    )
                    SubScreen.SETTINGS -> SettingsScreen(
                        currentProfile = drivingProfile,
                        onSelectDrivingProfile = { prof -> viewModel.setDrivingProfile(prof) }
                    )
                    null -> {}
                }
            } else {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        vehicle = selectedVehicle,
                        connectionState = connectionState,
                        recentSamples = recentSamples,
                        activeAlerts = activeAlerts,
                        healthBreakdown = viewModel.getHealthBreakdown(),
                        onNavigateDiagnostics = { viewModel.currentScreen.value = AppScreen.DIAGNOSTICS },
                        onNavigateTelemetry = { viewModel.currentScreen.value = AppScreen.TELEMETRY },
                        onNavigateAi = { viewModel.currentScreen.value = AppScreen.AI_ASSISTANT },
                        onNavigateMaintenance = { viewModel.currentSubScreen.value = SubScreen.MAINTENANCE },
                        onNavigateConnect = { viewModel.currentSubScreen.value = SubScreen.OBD_CONNECT },
                        onDismissAlert = { id -> viewModel.dismissAlert(id) }
                    )
                    AppScreen.TELEMETRY -> TelemetryScreen(
                        samples = recentSamples,
                        connectionState = connectionState
                    )
                    AppScreen.DIAGNOSTICS -> DiagnosticsScreen(
                        vehicle = selectedVehicle,
                        latestScan = latestDtcScan,
                        isScanning = isScanningDtc,
                        onStartScan = { viewModel.startDtcScan() },
                        onClearCodes = { viewModel.clearDtcCodes() }
                    )
                    AppScreen.AI_ASSISTANT -> AiAssistantScreen(
                        vehicle = selectedVehicle,
                        messages = aiMessages,
                        isLoading = isAiLoading,
                        isListening = isListening,
                        recentSamples = recentSamples,
                        onSendMessage = { query -> viewModel.sendAiQuery(query) },
                        onStartVoiceInput = { viewModel.startVoiceInput() },
                        onStopVoiceInput = { viewModel.stopVoiceInput() },
                        onSpeakText = { txt -> viewModel.speakText(txt) }
                    )
                    AppScreen.GARAGE -> GarageScreen(
                        vehicles = allVehicles,
                        selectedVehicleId = selectedVehicle?.id,
                        onSelectVehicle = { id -> viewModel.selectVehicle(id) },
                        onAddVehicle = { name, make, model, year, fuel, engine, trans, vin, notes ->
                            viewModel.addVehicle(name, make, model, year, fuel, engine, trans, vin, notes)
                        },
                        onDeleteVehicle = { id -> viewModel.deleteVehicle(id) }
                    )
                }
            }
        }
    }
}
