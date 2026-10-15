package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAtEpochMs DESC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE isSelected = 1 LIMIT 1")
    fun getSelectedVehicle(): Flow<VehicleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicle(vehicleId: String)

    @Query("UPDATE vehicles SET isSelected = 0")
    suspend fun clearSelected()

    @Query("UPDATE vehicles SET isSelected = 1 WHERE id = :vehicleId")
    suspend fun setSelected(vehicleId: String)

    @Transaction
    suspend fun selectVehicle(vehicleId: String) {
        clearSelected()
        setSelected(vehicleId)
    }
}

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM telemetry_samples WHERE vehicleId = :vehicleId ORDER BY capturedAtUtcEpochMs DESC LIMIT :limit")
    fun getRecentSamples(vehicleId: String, limit: Int = 100): Flow<List<TelemetrySampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: TelemetrySampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TelemetrySampleEntity>)

    @Query("DELETE FROM telemetry_samples WHERE capturedAtUtcEpochMs < :cutoffTimeEpochMs")
    suspend fun pruneOldSamples(cutoffTimeEpochMs: Long)
}

@Dao
interface DiagnosticsDao {
    @Query("SELECT * FROM diagnostic_scans WHERE vehicleId = :vehicleId ORDER BY scannedAtUtcEpochMs DESC")
    fun getScansForVehicle(vehicleId: String): Flow<List<DiagnosticScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: DiagnosticScanEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE vehicleId = :vehicleId AND isResolved = 0 ORDER BY detectedAtUtcEpochMs DESC")
    fun getActiveAlerts(vehicleId: String): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isResolved = 1 WHERE id = :alertId")
    suspend fun resolveAlert(alertId: String)

    @Query("DELETE FROM alerts WHERE vehicleId = :vehicleId")
    suspend fun clearAlerts(vehicleId: String)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startTimeUtcEpochMs DESC")
    fun getTripsForVehicle(vehicleId: String): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY serviceDateUtcEpochMs DESC")
    fun getMaintenanceForVehicle(vehicleId: String): Flow<List<MaintenanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MaintenanceRecordEntity)

    @Query("DELETE FROM maintenance_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: String)
}

@Dao
interface AiDao {
    @Query("SELECT * FROM ai_messages WHERE vehicleId = :vehicleId ORDER BY timestampUtcEpochMs ASC")
    fun getMessagesForVehicle(vehicleId: String): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE vehicleId = :vehicleId")
    suspend fun clearMessagesForVehicle(vehicleId: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY queuedAtUtcEpochMs ASC LIMIT 50")
    suspend fun getPendingQueue(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)
}
