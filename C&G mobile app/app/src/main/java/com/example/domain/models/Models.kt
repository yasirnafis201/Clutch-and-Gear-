package com.example.domain.models

// ─────────────────────────────────────────────────────────────────────────────
// Vehicle Profile
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a user's vehicle in the local garage.
 * Fuel type and transmission are stored as free-form strings for forward
 * compatibility with new vehicle types (e.g. hydrogen, dual-clutch).
 */
data class VehicleProfile(
    val id: String,
    val displayName: String,
    val make: String,
    val model: String,
    val year: Int,
    val fuelType: String = "Gasoline",
    val engineDetails: String = "",
    val transmission: String = "Manual",
    val vin: String = "",
    val preferredUnits: String = "Metric",
    val notes: String = "",
    val isSelected: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$year $make $model"
}

// ─────────────────────────────────────────────────────────────────────────────
// Telemetry / Vehicle Sample
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Denotes whether a VehicleSample carries live, reliable data or is degraded.
 *
 * This is surfaced in the UI to distinguish Measured (VALID) from
 * Estimated / error values so users are never deceived about data quality.
 */
enum class SampleValidity {
    /** OBD response was received and decoded successfully. */
    VALID,
    /** Response arrived but could not be decoded (parse error, unexpected bytes). */
    INVALID,
    /** Adapter disconnected — value is the last known reading. */
    STALE,
    /**
     * Vehicle ECU returned NO DATA / not supported for this PID.
     * The UI should show "N/A" rather than any numeric value.
     */
    UNSUPPORTED,
    /** Adapter returned an explicit error string (e.g. "CAN ERROR"). */
    ADAPTER_ERROR
}

/**
 * A single decoded OBD-II parameter reading, timestamped and tagged with
 * its data quality so the UI can apply appropriate badges.
 */
data class VehicleSample(
    val vehicleId: String,
    val sessionId: String,
    val parameterId: String,        // e.g. "010C", "0105"
    val parameterName: String,      // e.g. "Engine RPM", "Coolant Temperature"
    val value: Double,
    val unit: String,               // e.g. "rpm", "degrees C", "km/h"
    val capturedAtUtcEpochMs: Long,
    val receivedAtUtcEpochMs: Long = capturedAtUtcEpochMs,
    val sourceAdapter: String = "",
    val validity: SampleValidity = SampleValidity.VALID
) {
    val isLive: Boolean get() = validity == SampleValidity.VALID
    val isStale: Boolean get() = validity == SampleValidity.STALE
    val isUnavailable: Boolean get() =
        validity == SampleValidity.UNSUPPORTED || validity == SampleValidity.ADAPTER_ERROR
}

// ─────────────────────────────────────────────────────────────────────────────
// Diagnostic Trouble Codes
// ─────────────────────────────────────────────────────────────────────────────

/** Whether a DTC is actively stored, pending (intermittent), or permanent. */
enum class DtcType {
    /** Mode 03 - confirmed, stored in ECU non-volatile memory. */
    STORED,
    /** Mode 07 - detected but not yet confirmed over two drive cycles. */
    PENDING,
    /** Mode 0A - cannot be cleared without repair confirmation. */
    PERMANENT
}

/**
 * User-facing severity for a trouble code.
 * CRITICAL is not an OBD-II concept — it is assigned by local C&G heuristics
 * (e.g. overheating + P0115 combination).
 */
enum class DtcSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * A decoded OBD-II Diagnostic Trouble Code with human-readable context.
 *
 * [possibleCauses] and [recommendedActions] come from the local DtcDictionary
 * and are clearly informational — they must NOT be presented as definitive diagnoses.
 */
data class DiagnosticCode(
    val code: String,
    val category: String,                   // Powertrain | Chassis | Body | Network
    val description: String,
    val codeType: DtcType = DtcType.STORED,
    val severity: DtcSeverity = DtcSeverity.MEDIUM,
    val possibleCauses: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList()
)

/**
 * A complete DTC scan result, containing all codes retrieved in one session
 * and metadata about when the scan was performed.
 */
data class DiagnosticScanResult(
    val id: String,
    val vehicleId: String,
    val sessionId: String,
    val milActive: Boolean,             // Check-engine lamp status
    val scannedAtEpochMs: Long,
    val codes: List<DiagnosticCode>,
    val notes: String = ""
) {
    val hasActiveCodes: Boolean get() = codes.isNotEmpty()
    val codeCount: Int get() = codes.size
    val criticalCount: Int get() = codes.count { it.severity == DtcSeverity.CRITICAL }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vehicle Alerts
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A rule-triggered diagnostic alert generated by DiagnosticsEngine.
 * Alerts are displayed in the dashboard and persist until dismissed.
 */
data class VehicleAlert(
    val id: String,
    val vehicleId: String,
    val ruleId: String,
    val title: String,
    val message: String,
    val severity: DtcSeverity,
    val evidence: String,           // Human-readable raw sensor evidence
    val detectedAtEpochMs: Long,
    val isAcknowledged: Boolean = false,
    val isResolved: Boolean = false
) {
    val isActive: Boolean get() = !isAcknowledged && !isResolved
    val isUrgent: Boolean get() = severity == DtcSeverity.CRITICAL || severity == DtcSeverity.HIGH
}

// ─────────────────────────────────────────────────────────────────────────────
// Health Assessment
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Confidence level for the health assessment.
 * When data coverage is low, the score should NOT be displayed as authoritative.
 * The UI must surface this limitation clearly.
 */
enum class HealthConfidence {
    /** Less than 20% sensor coverage — too little data to assess. */
    INSUFFICIENT,
    /** 20-60% coverage — score is approximate. */
    LOW,
    /** 60-85% coverage — score is reasonably reliable. */
    MODERATE,
    /** Greater than 85% coverage — high confidence. */
    HIGH
}

/**
 * A single scored factor contributing to the overall vehicle health score.
 */
data class HealthFactor(
    val name: String,
    val score: Int,                     // 0-100
    val weight: Float,                  // 0.0-1.0, sum of all factors = 1.0
    val description: String,
    val confidence: HealthConfidence,
    val limitationNote: String? = null
)

/**
 * The complete explainable health breakdown displayed on the Health screen.
 *
 * [compositeScore] may be null when [confidence] is INSUFFICIENT —
 * callers must handle null and show an appropriate "not enough data" state.
 */
data class HealthBreakdown(
    val compositeScore: Int?,               // null if insufficient data
    val confidence: HealthConfidence,
    val factors: List<HealthFactor>,
    val activeDtcCount: Int,
    val dataCoveragePercent: Float,         // 0-100 percent
    val missingParameters: List<String>,
    val limitationNote: String? = null
) {
    val hasScore: Boolean get() = compositeScore != null

    val scoreCategory: String
        get() = when {
            compositeScore == null -> "unknown"
            compositeScore >= 85   -> "excellent"
            compositeScore >= 65   -> "good"
            compositeScore >= 45   -> "fair"
            else                   -> "poor"
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trip Records
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A summarised driving trip derived from a completed diagnostic session.
 * [distanceKm] is computed from speed samples integrated over time.
 */
data class TripRecord(
    val id: String,
    val vehicleId: String,
    val sessionId: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val distanceKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxRpm: Double = 0.0,
    val sampleCount: Int = 0
) {
    val durationMs: Long get() = endTimeEpochMs - startTimeEpochMs
    val durationMinutes: Long get() = durationMs / 60_000L
    val isDistanceReliable: Boolean get() = sampleCount > 30 && distanceKm > 0.0
}

// ─────────────────────────────────────────────────────────────────────────────
// Maintenance Records
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A logged vehicle service event.
 * [nextDueDateEpochMs] and [nextDueKm] are optional — when null, no reminder
 * will be generated for this record.
 */
data class MaintenanceRecord(
    val id: String,
    val vehicleId: String,
    val serviceDateEpochMs: Long,
    val odometerKm: Double = 0.0,
    val category: String,
    val workPerformed: String,
    val partsReplaced: String = "",
    val cost: Double = 0.0,
    val currency: String = "USD",
    val nextDueDateEpochMs: Long? = null,
    val nextDueKm: Double? = null,
    val notes: String = ""
) {
    val isOverdue: Boolean
        get() {
            val now = System.currentTimeMillis()
            return nextDueDateEpochMs != null && now > nextDueDateEpochMs
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// AI Assistant Messages
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single message in an AI assistant conversation.
 * [evidenceReferences] contains the grounded sensor/DTC data injected as context
 * so the UI can display evidence chips alongside the response.
 */
data class AiChatMessage(
    val id: String,
    val role: String,               // "user" | "assistant" | "system"
    val content: String,
    val evidenceReferences: List<String> = emptyList(),
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
) {
    val isUser: Boolean get() = role == "user"
    val isAssistant: Boolean get() = role == "assistant"
    val hasError: Boolean get() = errorMessage != null
}
