package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.VehicleRepository
import com.example.domain.models.AiChatMessage
import com.example.domain.models.DiagnosticScanResult
import com.example.domain.models.HealthBreakdown
import com.example.domain.models.MaintenanceRecord
import com.example.domain.models.TripRecord
import com.example.domain.models.VehicleAlert
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import com.example.platform.obd.AdapterDevice
import com.example.platform.obd.AdapterTransport
import com.example.platform.obd.ConnectionState
import com.example.platform.obd.DrivingProfile
import com.example.services.SpeechService
import com.example.ui.components.AppScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class SubScreen {
    TRIPS,
    MAINTENANCE,
    OBD_CONNECT,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = VehicleRepository(application)
    val speechService = SpeechService(application)

    val currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentSubScreen = MutableStateFlow<SubScreen?>(null)
    val showOnboarding = MutableStateFlow(false)

    val allVehicles: StateFlow<List<VehicleProfile>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicle: StateFlow<VehicleProfile?> = repository.selectedVehicle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val connectionState: StateFlow<ConnectionState> = repository.adapterManager.connectionState

    private val _recentSamples = MutableStateFlow<List<VehicleSample>>(emptyList())
    val recentSamples: StateFlow<List<VehicleSample>> = _recentSamples.asStateFlow()

    val activeAlerts: StateFlow<List<VehicleAlert>> = repository.getActiveAlerts()
    val latestDtcScan: StateFlow<DiagnosticScanResult?> = repository.latestDtcScan

    val trips: StateFlow<List<TripRecord>> = repository.trips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = repository.maintenanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                id = "init_01",
                role = "assistant",
                content = "Hello! I am your Clutch & Gear Vehicle Intelligence Assistant. I am grounded directly in your active OBD-II sensor stream and historical service records. Ask me anything about engine health, trouble codes, or maintenance recommendations."
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    val isScanningDtc = MutableStateFlow(false)
    val isAiLoading = MutableStateFlow(false)
    val isListening = speechService.isListening

    private val _availableDevices = MutableStateFlow<List<AdapterDevice>>(emptyList())
    val availableDevices: StateFlow<List<AdapterDevice>> = _availableDevices.asStateFlow()

    val currentTransport = MutableStateFlow(AdapterTransport.SIMULATOR)
    val drivingProfile = MutableStateFlow(DrivingProfile.CITY_CRUISE)

    init {
        // Collect telemetry stream
        viewModelScope.launch {
            repository.adapterManager.telemetryFlow.collect { sample ->
                val list = _recentSamples.value.toMutableList()
                list.removeAll { it.parameterId == sample.parameterId }
                list.add(0, sample)
                _recentSamples.value = list.take(15)
            }
        }

        // Auto-connect to simulator initially for immediate interactive experience
        viewModelScope.launch {
            val dev = AdapterDevice("SIM:00:11:22", "C&G Virtual Telemetry Rig", transport = AdapterTransport.SIMULATOR)
            repository.adapterManager.connect(dev)
            discoverDevices()
        }
    }

    fun getHealthBreakdown(): HealthBreakdown {
        return repository.getHealthBreakdown(connectionState.value == ConnectionState.CONNECTED)
    }

    fun startDtcScan() {
        val veh = selectedVehicle.value ?: return
        viewModelScope.launch {
            isScanningDtc.value = true
            repository.performDtcScan(veh.id)
            isScanningDtc.value = false
        }
    }

    fun clearDtcCodes() {
        viewModelScope.launch {
            repository.clearTroubleCodes()
        }
    }

    fun dismissAlert(alertId: String) {
        repository.diagnosticsEngine.dismissAlert(alertId)
    }

    fun sendAiQuery(query: String) {
        val veh = selectedVehicle.value ?: return
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = query
        )
        _aiMessages.value = _aiMessages.value + userMsg
        isAiLoading.value = true

        viewModelScope.launch {
            val response = repository.queryAiAssistant(
                query = query,
                vehicle = veh,
                isLiveConnected = connectionState.value == ConnectionState.CONNECTED
            )
            val assistantMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = "${response.summary}\n\nInterpretations:\n${response.interpretations.joinToString("\n") { "• $it" }}\n\nNext Steps:\n${response.nextSteps.joinToString("\n") { "→ $it" }}\n\nNotice: ${response.uncertainty}",
                evidenceReferences = response.evidence
            )
            _aiMessages.value = _aiMessages.value + assistantMsg
            isAiLoading.value = false
        }
    }

    fun startVoiceInput() {
        speechService.startListening(
            onResult = { spokenText ->
                sendAiQuery(spokenText)
            },
            onError = { err ->
                // fallback or notification
            }
        )
    }

    fun stopVoiceInput() {
        speechService.stopListening()
    }

    fun speakText(text: String) {
        speechService.speak(text)
    }

    fun selectVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.selectVehicle(vehicleId)
        }
    }

    fun addVehicle(
        displayName: String,
        make: String,
        model: String,
        year: Int,
        fuelType: String,
        engineDetails: String,
        transmission: String,
        vin: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addVehicle(displayName, make, model, year, fuelType, engineDetails, transmission, vin, notes)
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId)
        }
    }

    fun addMaintenanceRecord(
        odometerKm: Double,
        category: String,
        workPerformed: String,
        partsReplaced: String,
        cost: Double,
        nextDueKm: Double?,
        notes: String
    ) {
        val vehId = selectedVehicle.value?.id ?: "veh_default_miata_01"
        viewModelScope.launch {
            repository.addMaintenanceRecord(vehId, odometerKm, category, workPerformed, partsReplaced, cost, nextDueKm, notes)
        }
    }

    fun discoverDevices() {
        viewModelScope.launch {
            _availableDevices.value = repository.adapterManager.discoverDevices()
        }
    }

    fun connectDevice(device: AdapterDevice) {
        viewModelScope.launch {
            repository.adapterManager.connect(device)
        }
    }

    fun disconnectAdapter() {
        viewModelScope.launch {
            repository.adapterManager.disconnect()
        }
    }

    fun selectTransport(transport: AdapterTransport) {
        currentTransport.value = transport
        repository.adapterManager.selectAdapterType(transport)
        discoverDevices()
    }

    fun setDrivingProfile(profile: DrivingProfile) {
        drivingProfile.value = profile
        repository.adapterManager.fakeAdapter.drivingProfile = profile
    }

    override fun onCleared() {
        super.onCleared()
        speechService.dispose()
        repository.adapterManager.getActiveAdapter().dispose()
    }
}
