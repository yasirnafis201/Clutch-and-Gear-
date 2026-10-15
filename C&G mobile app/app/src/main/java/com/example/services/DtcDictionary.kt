package com.example.services

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.DtcType

object DtcDictionary {

    data class DtcInfo(
        val code: String,
        val category: String,
        val description: String,
        val severity: DtcSeverity,
        val symptoms: List<String>,
        val possibleCauses: List<String>,
        val recommendedActions: List<String>
    )

    private val DICTIONARY = mapOf(
        "P0115" to DtcInfo(
            "P0115", "Powertrain", "Engine Coolant Temperature Circuit Malfunction",
            DtcSeverity.HIGH,
            listOf("Erratic temperature gauge", "Engine overheating warning", "Poor fuel economy", "Radiator fan running constantly"),
            listOf("Defective ECT sensor", "Corroded sensor connector pins", "Open or shorted harness wiring", "Low engine coolant level"),
            listOf("Check coolant level when engine is cool", "Inspect ECT sensor connector for moisture or corrosion", "Measure sensor resistance with multimeter")
        ),
        "P0300" to DtcInfo(
            "P0300", "Powertrain", "Random / Multiple Cylinder Misfire Detected",
            DtcSeverity.HIGH,
            listOf("Rough engine idle", "Hesitation during acceleration", "Flashing Check Engine Light", "Raw fuel odor from exhaust"),
            listOf("Worn spark plugs", "Failing ignition coil packs", "Low fuel pressure", "Intake manifold vacuum leak"),
            listOf("Inspect spark plug electrode gap and fouling", "Test coil packs for resistance", "Check fuel pressure regulator and vacuum lines")
        ),
        "P0420" to DtcInfo(
            "P0420", "Powertrain", "Catalyst System Efficiency Below Threshold (Bank 1)",
            DtcSeverity.MEDIUM,
            listOf("Check engine light on", "Slight drop in fuel efficiency", "Sulfur or rotten-egg exhaust odor"),
            listOf("Aged catalytic converter substrate", "Faulty downstream heated O2 sensor", "Exhaust leak near converter"),
            listOf("Verify downstream O2 sensor voltage waveform", "Inspect exhaust pipe for cracks or pinholes before replacing catalyst")
        ),
        "P0171" to DtcInfo(
            "P0171", "Powertrain", "System Too Lean (Bank 1)",
            DtcSeverity.MEDIUM,
            listOf("Engine hesitation on throttle tip-in", "Rough idle", "Pinging or detonation under load"),
            listOf("Contaminated MAF sensor element", "Cracked intake vacuum hose or PCV boot", "Weak fuel pump", "Clogged fuel filter"),
            listOf("Clean Mass Air Flow sensor with electronics cleaner", "Perform smoke test for vacuum leaks", "Check fuel rail pressure")
        ),
        "P0128" to DtcInfo(
            "P0128", "Powertrain", "Coolant Thermostat (Coolant Temp Below Regulating Temp)",
            DtcSeverity.LOW,
            listOf("Heater blows lukewarm air", "Slow engine warmup", "Reduced fuel mileage"),
            listOf("Thermostat stuck open", "Defective thermostat seal", "ECT sensor reading low"),
            listOf("Replace engine thermostat and flush coolant", "Inspect thermostat housing gasket")
        ),
        "P0500" to DtcInfo(
            "P0500", "Powertrain", "Vehicle Speed Sensor Malfunction",
            DtcSeverity.MEDIUM,
            listOf("Inaccurate or dead speedometer", "Erratic transmission shifting", "ABS light illuminated"),
            listOf("Faulty vehicle speed sensor (VSS)", "Broken wiring to transmission sensor", "Damaged instrument cluster"),
            listOf("Inspect VSS sensor connector and wiring harness", "Scan transmission control module for secondary codes")
        ),
        "C0035" to DtcInfo(
            "C0035", "Chassis", "Left Front Wheel Speed Sensor Circuit",
            DtcSeverity.MEDIUM,
            listOf("ABS warning light on", "Traction control disabled", "Unwanted ABS actuation at low speed"),
            listOf("Damaged wheel speed sensor cable", "Cracked tone ring / reluctor wheel", "Wheel bearing play"),
            listOf("Inspect left front sensor wiring near suspension strut", "Clean magnetic tone ring")
        ),
        "B0001" to DtcInfo(
            "B0001", "Body", "Driver Airbag Squib Circuit Low",
            DtcSeverity.HIGH,
            listOf("Airbag SRS warning lamp on"),
            listOf("Clock spring ribbon cable fracture", "Airbag squib connector loose"),
            listOf("Have certified SRS technician inspect steering column clock spring")
        ),
        "U0100" to DtcInfo(
            "U0100", "Network", "Lost Communication with Engine Control Module (ECM/PCM)",
            DtcSeverity.CRITICAL,
            listOf("Vehicle may crank but not start", "Multiple warning lights on dashboard", "Communication failure on scan tool"),
            listOf("Main ECM fuse blown or relay failing", "CAN bus wiring short or high resistance", "Chassis ground strap corroded"),
            listOf("Inspect ECM main power fuse and ground cables", "Test CAN High and CAN Low terminating resistor (60 ohms)")
        )
    )

    fun lookup(code: String): DtcInfo? = DICTIONARY[code.uppercase().trim()]

    fun toDiagnosticCode(code: String, type: DtcType = DtcType.STORED): DiagnosticCode {
        val info = lookup(code)
        return if (info != null) {
            DiagnosticCode(
                code = info.code,
                category = info.category,
                description = info.description,
                codeType = type,
                severity = info.severity,
                possibleCauses = info.possibleCauses,
                recommendedActions = info.recommendedActions
            )
        } else {
            val prefix = code.take(1).uppercase()
            val category = when (prefix) {
                "P" -> "Powertrain"
                "C" -> "Chassis"
                "B" -> "Body"
                "U" -> "Network"
                else -> "General"
            }
            DiagnosticCode(
                code = code.uppercase(),
                category = category,
                description = "Standard OBD-II Fault Code $code",
                codeType = type,
                severity = DtcSeverity.MEDIUM,
                possibleCauses = listOf("Sensor out of specification", "Wiring harness defect"),
                recommendedActions = listOf("Perform guided circuit testing", "Consult factory repair manual")
            )
        }
    }
}
