package com.example.services

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.VehicleAlert
import com.example.domain.models.VehicleSample
import com.example.platform.obd.PidRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DiagnosticsEngine {

    private val _activeAlerts = MutableStateFlow<List<VehicleAlert>>(emptyList())
    val activeAlerts: StateFlow<List<VehicleAlert>> = _activeAlerts.asStateFlow()

    private val lastAlertTime = mutableMapOf<String, Long>()
    private val DEBOUNCE_MS = 45_000L // 45 seconds debounce between duplicate alerts

    private var lastTelemetryReceivedEpochMs: Long = 0
    private var currentRpm: Double = 0.0

    /**
     * Evaluates incoming telemetry samples against deterministic threshold rules.
     */
    fun evaluateSample(sample: VehicleSample, vehicleId: String) {
        val now = System.currentTimeMillis()
        lastTelemetryReceivedEpochMs = now

        if (sample.parameterId == PidRegistry.PID_ENGINE_RPM) {
            currentRpm = sample.value
        }

        // Rule 1: High Coolant Temperature Threshold (> 105°C)
        if (sample.parameterId == PidRegistry.PID_COOLANT_TEMP) {
            if (sample.value >= 105.0) {
                triggerAlert(
                    ruleId = "RULE_COOLANT_OVERHEAT",
                    vehicleId = vehicleId,
                    title = "Elevated Coolant Temperature",
                    message = "Engine coolant reached ${sample.value}°C (normal range is 85°C–98°C). Continued operation may cause cylinder head warpage.",
                    severity = if (sample.value >= 115.0) DtcSeverity.CRITICAL else DtcSeverity.HIGH,
                    evidence = "PID 0105 reading ${sample.value}°C at RPM ${currentRpm.toInt()}"
                )
            }
        }

        // Rule 2: Alternator / Charging System Voltage Anomaly (< 12.2V with engine running)
        if (sample.parameterId == PidRegistry.PID_MODULE_VOLTAGE) {
            if (currentRpm > 600.0 && sample.value < 12.2) {
                triggerAlert(
                    ruleId = "RULE_LOW_CHARGING_VOLTAGE",
                    vehicleId = vehicleId,
                    title = "Weak Charging System Voltage",
                    message = "System voltage is ${sample.value}V while engine is running. Alternator output or battery charging may be compromised.",
                    severity = DtcSeverity.MEDIUM,
                    evidence = "PID 0142 recorded ${sample.value}V at ${currentRpm.toInt()} RPM (expected > 13.5V)"
                )
            }
        }

        // Rule 3: High Sustained Engine Load (> 95%)
        if (sample.parameterId == PidRegistry.PID_ENGINE_LOAD) {
            if (sample.value >= 95.0) {
                triggerAlert(
                    ruleId = "RULE_HIGH_ENGINE_LOAD",
                    vehicleId = vehicleId,
                    title = "Extreme Engine Load",
                    message = "Engine calculated load reached ${sample.value.toInt()}%. Ensure cooling fans are operating normally.",
                    severity = DtcSeverity.LOW,
                    evidence = "PID 0104 recorded ${sample.value.toInt()}% load"
                )
            }
        }
    }

    /**
     * Evaluates scan results for newly logged Diagnostic Trouble Codes.
     */
    fun evaluateScanCodes(codes: List<DiagnosticCode>, vehicleId: String) {
        for (code in codes) {
            triggerAlert(
                ruleId = "RULE_DTC_${code.code}",
                vehicleId = vehicleId,
                title = "Diagnostic Trouble Code: ${code.code}",
                message = "${code.description}. Severity: ${code.severity.name}.",
                severity = code.severity,
                evidence = "ECU logged code ${code.code} (${code.codeType.name}) in ${code.category} system"
            )
        }
    }

    /**
     * Evaluates whether telemetry has become stale.
     */
    fun checkTelemetryFreshness(isConnected: Boolean, vehicleId: String) {
        if (!isConnected || lastTelemetryReceivedEpochMs == 0L) return
        val elapsed = System.currentTimeMillis() - lastTelemetryReceivedEpochMs
        if (elapsed > 4000L) { // 4 seconds without samples
            triggerAlert(
                ruleId = "RULE_STALE_TELEMETRY",
                vehicleId = vehicleId,
                title = "OBD-II Telemetry Stalled",
                message = "No sensor data received for ${elapsed / 1000}s. Check adapter Bluetooth connection.",
                severity = DtcSeverity.LOW,
                evidence = "Telemetry stream silent for ${elapsed}ms"
            )
        }
    }

    fun dismissAlert(alertId: String) {
        _activeAlerts.value = _activeAlerts.value.filter { it.id != alertId }
    }

    fun clearAllAlerts() {
        _activeAlerts.value = emptyList()
    }

    private fun triggerAlert(
        ruleId: String,
        vehicleId: String,
        title: String,
        message: String,
        severity: DtcSeverity,
        evidence: String
    ) {
        val now = System.currentTimeMillis()
        val lastTime = lastAlertTime[ruleId] ?: 0L
        if (now - lastTime < DEBOUNCE_MS) {
            return // Deduplicate
        }
        lastAlertTime[ruleId] = now

        val newAlert = VehicleAlert(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            ruleId = ruleId,
            title = title,
            message = message,
            severity = severity,
            evidence = evidence,
            detectedAtEpochMs = now
        )

        // Prepend new alert, keep latest 20
        _activeAlerts.value = (listOf(newAlert) + _activeAlerts.value).take(20)
    }
}
