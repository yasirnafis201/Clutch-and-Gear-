package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
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
)

@Entity(tableName = "telemetry_samples")
data class TelemetrySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: String,
    val sessionId: String,
    val parameterId: String,
    val parameterName: String,
    val value: Double,
    val unit: String,
    val capturedAtUtcEpochMs: Long,
    val sourceAdapter: String,
    val validity: String = "VALID"
)

@Entity(tableName = "diagnostic_scans")
data class DiagnosticScanEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val sessionId: String,
    val milActive: Boolean,
    val scannedAtUtcEpochMs: Long,
    val codesJson: String,
    val notes: String = ""
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val ruleId: String,
    val title: String,
    val message: String,
    val severity: String,
    val evidence: String,
    val detectedAtUtcEpochMs: Long,
    val isAcknowledged: Boolean = false,
    val isResolved: Boolean = false
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val sessionId: String,
    val startTimeUtcEpochMs: Long,
    val endTimeUtcEpochMs: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val maxRpm: Double,
    val sampleCount: Int
)

@Entity(tableName = "maintenance_records")
data class MaintenanceRecordEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val serviceDateUtcEpochMs: Long,
    val odometerKm: Double,
    val category: String,
    val workPerformed: String,
    val partsReplaced: String = "",
    val cost: Double = 0.0,
    val currency: String = "USD",
    val nextDueDateUtcEpochMs: Long? = null,
    val nextDueKm: Double? = null,
    val notes: String = ""
)

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val role: String,
    val content: String,
    val evidenceJson: String = "",
    val timestampUtcEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val queuedAtUtcEpochMs: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
