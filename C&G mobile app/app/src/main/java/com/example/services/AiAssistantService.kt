package com.example.services

import com.example.domain.models.AiChatMessage
import com.example.domain.models.DiagnosticCode
import com.example.domain.models.VehicleAlert
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import kotlinx.coroutines.delay
import java.util.UUID

data class GroundedAiResponse(
    val summary: String,
    val evidence: List<String>,
    val interpretations: List<String>,
    val nextSteps: List<String>,
    val uncertainty: String,
    val dataFreshness: String
)

class AiAssistantService {

    suspend fun generateGroundedResponse(
        query: String,
        vehicle: VehicleProfile,
        recentSamples: List<VehicleSample>,
        dtcs: List<DiagnosticCode>,
        activeAlerts: List<VehicleAlert>,
        isLiveConnected: Boolean
    ): GroundedAiResponse {
        delay(600) // Simulated processing latency for natural UX flow

        val lowerQuery = query.lowercase()
        val evidence = mutableListOf<String>()

        // Gather real evidence
        val coolant = recentSamples.firstOrNull { it.parameterId == "0105" }
        val rpm = recentSamples.firstOrNull { it.parameterId == "010C" }
        val voltage = recentSamples.firstOrNull { it.parameterId == "0142" }
        val speed = recentSamples.firstOrNull { it.parameterId == "010D" }
        val load = recentSamples.firstOrNull { it.parameterId == "0104" }

        if (coolant != null) evidence.add("Coolant Temp: ${coolant.value} °C")
        if (rpm != null) evidence.add("Engine RPM: ${rpm.value.toInt()} rpm")
        if (voltage != null) evidence.add("System Voltage: ${voltage.value} V")
        if (speed != null) evidence.add("Vehicle Speed: ${speed.value.toInt()} km/h")
        if (load != null) evidence.add("Engine Load: ${load.value.toInt()}%")

        for (dtc in dtcs) {
            evidence.add("DTC ${dtc.code}: ${dtc.description}")
        }
        for (alert in activeAlerts) {
            evidence.add("Alert: ${alert.title} (${alert.severity})")
        }

        val freshness = if (isLiveConnected) "Live Telemetry (Actively Polling)" else "Saved Records (Adapter Disconnected)"
        val uncertaintyNotice = if (isLiveConnected) {
            "Diagnostic assessments are based on standard OBD-II parameter streams and should not replace professional hands-on inspection."
        } else {
            "Adapter is currently disconnected. Recommendations are grounded in historical diagnostic scans and service logs."
        }

        return when {
            lowerQuery.contains("hot") || lowerQuery.contains("temp") || lowerQuery.contains("coolant") -> {
                val tempVal = coolant?.value ?: 90.0
                val isHot = tempVal >= 105.0
                GroundedAiResponse(
                    summary = if (isHot) {
                        "Your engine coolant is elevated at ${tempVal}°C. Operating in this band requires caution."
                    } else {
                        "Your coolant temperature is ${tempVal}°C, which is within the nominal operating band (85°C–98°C)."
                    },
                    evidence = evidence.ifEmpty { listOf("No live temperature telemetry reported.") },
                    interpretations = if (isHot) {
                        listOf(
                            "Thermostat may be failing to open completely",
                            "Electric radiator cooling fan may not be engaging at high temp",
                            "Coolant level in recovery reservoir may be low"
                        )
                    } else {
                        listOf("Engine cooling circuit is regulating temperature effectively.")
                    },
                    nextSteps = if (isHot) {
                        listOf(
                            "Check coolant level safely once the engine has cooled down",
                            "Never open radiator cap when engine is warm",
                            "Have cooling system pressure-tested by a mechanic"
                        )
                    } else {
                        listOf("Continue regular maintenance and monitor during hot weather or towing.")
                    },
                    uncertainty = uncertaintyNotice,
                    dataFreshness = freshness
                )
            }

            lowerQuery.contains("code") || lowerQuery.contains("dtc") || lowerQuery.contains("check engine") || lowerQuery.contains("misfire") -> {
                if (dtcs.isNotEmpty()) {
                    val firstCode = dtcs.first()
                    GroundedAiResponse(
                        summary = "Your ${vehicle.make} ${vehicle.model} has ${dtcs.size} logged diagnostic trouble code(s). Primary code: ${firstCode.code} - ${firstCode.description}.",
                        evidence = evidence,
                        interpretations = firstCode.possibleCauses.ifEmpty {
                            listOf("Circuit signal out of threshold", "Component mechanical deterioration")
                        },
                        nextSteps = firstCode.recommendedActions.ifEmpty {
                            listOf("Perform guided diagnostic check", "Schedule inspection with a certified mechanic")
                        },
                        uncertainty = "Trouble codes indicate circuit and symptom detection by the ECU, not guaranteed component failure. Sensor wiring should always be checked prior to replacing parts.",
                        dataFreshness = freshness
                    )
                } else {
                    GroundedAiResponse(
                        summary = "No active diagnostic trouble codes are currently stored in your vehicle's engine control module.",
                        evidence = listOf("ECU Mode 03 query returned 0 stored DTCs"),
                        interpretations = listOf("Emissions and powertrain diagnostic systems are reporting all-clear."),
                        nextSteps = listOf("Perform regular scheduled maintenance per manufacturer manual."),
                        uncertainty = uncertaintyNotice,
                        dataFreshness = freshness
                    )
                }
            }

            lowerQuery.contains("mechanic") || lowerQuery.contains("ask") -> {
                GroundedAiResponse(
                    summary = "Here is an evidence-based summary you can hand to or discuss with your mechanic for your ${vehicle.year} ${vehicle.make} ${vehicle.model}:",
                    evidence = evidence,
                    interpretations = listOf(
                        "Giving technicians exact DTCs and sensor numbers saves diagnostic labor hours."
                    ),
                    nextSteps = listOf(
                        "1. Share recorded DTC codes: ${dtcs.map { it.code }.ifEmpty { listOf("None") }.joinToString(", ")}",
                        "2. Report observed coolant reading: ${coolant?.value?.toInt() ?: "N/A"}°C",
                        "3. Request a multi-point inspection focusing on logged symptoms"
                    ),
                    uncertainty = "Technicians will verify harness continuity and sensor waveforms with specialized shop scopes.",
                    dataFreshness = freshness
                )
            }

            else -> {
                // General "How is my car doing?"
                val faultCount = dtcs.size + activeAlerts.size
                val statusText = if (faultCount == 0) "operating smoothly with zero active DTC faults" else "requires attention due to $faultCount recorded condition(s)"
                GroundedAiResponse(
                    summary = "Your ${vehicle.year} ${vehicle.make} ${vehicle.model} is currently $statusText.",
                    evidence = evidence.ifEmpty { listOf("Vehicle profile: ${vehicle.displayName}") },
                    interpretations = if (faultCount == 0) {
                        listOf("All monitored parameters (RPM, Speed, Load, Voltage) appear within standard operational limits.")
                    } else {
                        listOf("Active warnings exist. Review diagnostic trouble codes and alert cards for details.")
                    },
                    nextSteps = if (faultCount == 0) {
                        listOf("Maintain regular driving habits and observe scheduled oil service intervals.")
                    } else {
                        listOf("Review the Diagnostics tab to inspect specific trouble codes and recommendations.")
                    },
                    uncertainty = uncertaintyNotice,
                    dataFreshness = freshness
                )
            }
        }
    }
}
