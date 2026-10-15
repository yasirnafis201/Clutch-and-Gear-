package com.example

import com.example.domain.models.SampleValidity
import com.example.domain.models.VehicleSample
import com.example.platform.obd.PidRegistry
import com.example.services.DiagnosticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsEngineTest {

    @Test
    fun testCoolantOverheatAlertFires() {
        val engine = DiagnosticsEngine()
        val now = System.currentTimeMillis()

        val sampleOverheat = VehicleSample(
            vehicleId = "veh_01",
            sessionId = "sess_01",
            parameterId = PidRegistry.PID_COOLANT_TEMP,
            parameterName = "Coolant Temperature",
            value = 112.0,
            unit = "°C",
            capturedAtUtcEpochMs = now,
            sourceAdapter = "Simulator",
            validity = SampleValidity.VALID
        )

        engine.evaluateSample(sampleOverheat, "veh_01")
        val alerts = engine.activeAlerts.value
        assertEquals(1, alerts.size)
        assertEquals("RULE_COOLANT_OVERHEAT", alerts[0].ruleId)
    }

    @Test
    fun testNominalCoolantDoesNotAlert() {
        val engine = DiagnosticsEngine()
        val now = System.currentTimeMillis()

        val sampleNormal = VehicleSample(
            vehicleId = "veh_01",
            sessionId = "sess_01",
            parameterId = PidRegistry.PID_COOLANT_TEMP,
            parameterName = "Coolant Temperature",
            value = 92.0,
            unit = "°C",
            capturedAtUtcEpochMs = now,
            sourceAdapter = "Simulator",
            validity = SampleValidity.VALID
        )

        engine.evaluateSample(sampleNormal, "veh_01")
        val alerts = engine.activeAlerts.value
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testLowVoltageWithRunningEngineAlerts() {
        val engine = DiagnosticsEngine()
        val now = System.currentTimeMillis()

        // Set RPM to 1500 (engine running)
        engine.evaluateSample(
            VehicleSample(
                vehicleId = "veh_01",
                sessionId = "sess_01",
                parameterId = PidRegistry.PID_ENGINE_RPM,
                parameterName = "Engine RPM",
                value = 1500.0,
                unit = "rpm",
                capturedAtUtcEpochMs = now,
                sourceAdapter = "Simulator"
            ),
            "veh_01"
        )

        // Low voltage reading: 11.8V
        engine.evaluateSample(
            VehicleSample(
                vehicleId = "veh_01",
                sessionId = "sess_01",
                parameterId = PidRegistry.PID_MODULE_VOLTAGE,
                parameterName = "Module Voltage",
                value = 11.8,
                unit = "V",
                capturedAtUtcEpochMs = now,
                sourceAdapter = "Simulator"
            ),
            "veh_01"
        )

        val alerts = engine.activeAlerts.value
        assertEquals(1, alerts.size)
        assertEquals("RULE_LOW_CHARGING_VOLTAGE", alerts[0].ruleId)
    }
}
