package com.example.data.repository

import android.content.Context
import com.example.data.local.AlertEntity
import com.example.data.local.AppDatabase
import com.example.data.local.DiagnosticScanEntity
import com.example.data.local.MaintenanceRecordEntity
import com.example.data.local.TelemetrySampleEntity
import com.example.data.local.TripEntity
import com.example.data.local.VehicleEntity
import com.example.domain.models.AiChatMessage
import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DiagnosticScanResult
import com.example.domain.models.DtcSeverity
import com.example.domain.models.DtcType
import com.example.domain.models.HealthBreakdown
import com.example.domain.models.MaintenanceRecord
import com.example.domain.models.SampleValidity
import com.example.domain.models.TripRecord
import com.example.domain.models.VehicleAlert
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import com.example.platform.obd.AdapterDevice
import com.example.platform.obd.AdapterManager
import com.example.platform.obd.AdapterTransport
import com.example.platform.obd.ConnectionState
import com.example.services.AiAssistantService
import com.example.services.DiagnosticsEngine
import com.example.services.DtcDictionary
import com.example.services.GroundedAiResponse
import com.example.services.HealthScoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleRepository(
    private val context: Context,
    val adapterManager: AdapterManager = AdapterManager(context),
    val diagnosticsEngine: DiagnosticsEngine = DiagnosticsEngine(),
    val aiAssistantService: AiAssistantService = AiAssistantService()
) {
    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val allVehicles: Flow<List<VehicleProfile>> = db.vehicleDao().getAllVehicles().map { entities ->
        entities.map { it.toDomain() }
    }

    val selectedVehicle: Flow<VehicleProfile?> = db.vehicleDao().getSelectedVehicle().map { it?.toDomain() }

    val recentTelemetry: Flow<List<VehicleSample>> = db.telemetryDao().getRecentSamples("active_vehicle", 60).map { entities ->
        entities.map { it.toDomain() }
    }

    val trips: Flow<List<TripRecord>> = db.tripDao().getTripsForVehicle("veh_default_miata_01").map { entities ->
        entities.map { it.toDomain() }
    }

    val maintenanceRecords: Flow<List<MaintenanceRecord>> = db.maintenanceDao().getMaintenanceForVehicle("veh_default_miata_01").map { entities ->
        entities.map { it.toDomain() }
    }

    // Active in-memory and persisted DTCs
    private val _latestDtcScan = MutableStateFlow<DiagnosticScanResult?>(null)
    val latestDtcScan: StateFlow<DiagnosticScanResult?> = _latestDtcScan.asStateFlow()

    // Recent samples cache for health scoring
    private val recentSamplesCache = mutableListOf<VehicleSample>()

    init {
        // Collect adapter telemetry into DB & local diagnostics engine
        scope.launch {
            adapterManager.telemetryFlow.collect { sample ->
                synchronized(recentSamplesCache) {
                    recentSamplesCache.add(sample)
                    if (recentSamplesCache.size > 80) {
                        recentSamplesCache.removeAt(0)
                    }
                }
                diagnosticsEngine.evaluateSample(sample, "veh_default_miata_01")
            }
        }
    }

    fun getActiveAlerts(): StateFlow<List<VehicleAlert>> = diagnosticsEngine.activeAlerts

    fun getHealthBreakdown(isLiveConnected: Boolean): HealthBreakdown {
        val dtcs = latestDtcScan.value?.codes ?: emptyList()
        val samples = synchronized(recentSamplesCache) { recentSamplesCache.toList() }
        return HealthScoreService.computeHealth(
            dtcs = dtcs,
            recentSamples = samples,
            maintenanceRecords = emptyList(),
            isLiveConnected = isLiveConnected
        )
    }

    suspend fun addVehicle(
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
        val id = "veh_" + UUID.randomUUID().toString().take(8)
        db.vehicleDao().insertVehicle(
            VehicleEntity(
                id = id,
                displayName = displayName,
                make = make,
                model = model,
                year = year,
                fuelType = fuelType,
                engineDetails = engineDetails,
                transmission = transmission,
                vin = vin,
                notes = notes,
                isSelected = false,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun selectVehicle(vehicleId: String) {
        db.vehicleDao().selectVehicle(vehicleId)
    }

    suspend fun deleteVehicle(vehicleId: String) {
        db.vehicleDao().deleteVehicle(vehicleId)
    }

    suspend fun performDtcScan(vehicleId: String): DiagnosticScanResult {
        val codes = adapterManager.readDtcCodes()
        val scanResult = DiagnosticScanResult(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            sessionId = "scan_session",
            milActive = codes.isNotEmpty(),
            scannedAtEpochMs = System.currentTimeMillis(),
            codes = codes,
            notes = "OBD-II Mode 03 emission scan completed"
        )
        _latestDtcScan.value = scanResult
        diagnosticsEngine.evaluateScanCodes(codes, vehicleId)

        // Persist scan
        db.diagnosticsDao().insertScan(
            DiagnosticScanEntity(
                id = scanResult.id,
                vehicleId = vehicleId,
                sessionId = scanResult.sessionId,
                milActive = scanResult.milActive,
                scannedAtUtcEpochMs = scanResult.scannedAtEpochMs,
                codesJson = codes.joinToString(";") { "${it.code}:${it.description}" },
                notes = scanResult.notes
            )
        )

        return scanResult
    }

    suspend fun clearTroubleCodes(): Boolean {
        val success = adapterManager.clearDtcCodes()
        if (success) {
            _latestDtcScan.value = DiagnosticScanResult(
                id = UUID.randomUUID().toString(),
                vehicleId = "veh_default_miata_01",
                sessionId = "cleared_session",
                milActive = false,
                scannedAtEpochMs = System.currentTimeMillis(),
                codes = emptyList(),
                notes = "Trouble codes cleared by user confirmation"
            )
            diagnosticsEngine.clearAllAlerts()
        }
        return success
    }

    suspend fun addMaintenanceRecord(
        vehicleId: String,
        odometerKm: Double,
        category: String,
        workPerformed: String,
        partsReplaced: String,
        cost: Double,
        nextDueKm: Double?,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        db.maintenanceDao().insertRecord(
            MaintenanceRecordEntity(
                id = "maint_" + UUID.randomUUID().toString().take(8),
                vehicleId = vehicleId,
                serviceDateUtcEpochMs = now,
                odometerKm = odometerKm,
                category = category,
                workPerformed = workPerformed,
                partsReplaced = partsReplaced,
                cost = cost,
                nextDueDateUtcEpochMs = now + (180L * 24 * 3600 * 1000),
                nextDueKm = nextDueKm,
                notes = notes
            )
        )
    }

    suspend fun queryAiAssistant(
        query: String,
        vehicle: VehicleProfile,
        isLiveConnected: Boolean
    ): GroundedAiResponse {
        val dtcs = latestDtcScan.value?.codes ?: emptyList()
        val samples = synchronized(recentSamplesCache) { recentSamplesCache.toList() }
        val alerts = diagnosticsEngine.activeAlerts.value

        return aiAssistantService.generateGroundedResponse(
            query = query,
            vehicle = vehicle,
            recentSamples = samples,
            dtcs = dtcs,
            activeAlerts = alerts,
            isLiveConnected = isLiveConnected
        )
    }

    // Entity to Domain mappers
    private fun VehicleEntity.toDomain() = VehicleProfile(
        id = id,
        displayName = displayName,
        make = make,
        model = model,
        year = year,
        fuelType = fuelType,
        engineDetails = engineDetails,
        transmission = transmission,
        vin = vin,
        preferredUnits = preferredUnits,
        notes = notes,
        isSelected = isSelected,
        createdAtEpochMs = createdAtEpochMs
    )

    private fun TelemetrySampleEntity.toDomain() = VehicleSample(
        vehicleId = vehicleId,
        sessionId = sessionId,
        parameterId = parameterId,
        parameterName = parameterName,
        value = value,
        unit = unit,
        capturedAtUtcEpochMs = capturedAtUtcEpochMs,
        sourceAdapter = sourceAdapter,
        validity = SampleValidity.valueOf(validity)
    )

    private fun TripEntity.toDomain() = TripRecord(
        id = id,
        vehicleId = vehicleId,
        sessionId = sessionId,
        startTimeEpochMs = startTimeUtcEpochMs,
        endTimeEpochMs = endTimeUtcEpochMs,
        distanceKm = distanceKm,
        avgSpeedKmh = avgSpeedKmh,
        maxRpm = maxRpm,
        sampleCount = sampleCount
    )

    private fun MaintenanceRecordEntity.toDomain() = MaintenanceRecord(
        id = id,
        vehicleId = vehicleId,
        serviceDateEpochMs = serviceDateUtcEpochMs,
        odometerKm = odometerKm,
        category = category,
        workPerformed = workPerformed,
        partsReplaced = partsReplaced,
        cost = cost,
        currency = currency,
        nextDueDateEpochMs = nextDueDateUtcEpochMs,
        nextDueKm = nextDueKm,
        notes = notes
    )
}
