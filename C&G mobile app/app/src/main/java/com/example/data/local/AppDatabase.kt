package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        VehicleEntity::class,
        TelemetrySampleEntity::class,
        DiagnosticScanEntity::class,
        AlertEntity::class,
        TripEntity::class,
        MaintenanceRecordEntity::class,
        AiMessageEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun diagnosticsDao(): DiagnosticsDao
    abstract fun alertDao(): AlertDao
    abstract fun tripDao(): TripDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun aiDao(): AiDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clutch_and_gear.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            // Seed default vehicle
                            val defaultVehicleId = "veh_default_miata_01"
                            database.vehicleDao().insertVehicle(
                                VehicleEntity(
                                    id = defaultVehicleId,
                                    displayName = "2021 Mazda MX-5 Miata",
                                    make = "Mazda",
                                    model = "MX-5 Miata Club",
                                    year = 2021,
                                    fuelType = "Gasoline (93 Octane)",
                                    engineDetails = "2.0L SkyActiv-G I4 (181 hp)",
                                    transmission = "6-Speed Manual",
                                    vin = "JM1NDAD74M0123456",
                                    preferredUnits = "Metric",
                                    notes = "Track-day prepared, Bilstein dampening",
                                    isSelected = true,
                                    createdAtEpochMs = System.currentTimeMillis()
                                )
                            )
                            // Seed initial maintenance records
                            val now = System.currentTimeMillis()
                            database.maintenanceDao().insertRecord(
                                MaintenanceRecordEntity(
                                    id = "maint_01",
                                    vehicleId = defaultVehicleId,
                                    serviceDateUtcEpochMs = now - (30L * 24 * 3600 * 1000),
                                    odometerKm = 24500.0,
                                    category = "Engine Oil & Filter",
                                    workPerformed = "Full synthetic Motul 300V 0W-20 oil change & OEM Mazda filter replacement",
                                    partsReplaced = "Oil Filter, Crush Washer, 4.3L 0W-20",
                                    cost = 85.00,
                                    currency = "USD",
                                    nextDueDateUtcEpochMs = now + (90L * 24 * 3600 * 1000),
                                    nextDueKm = 30000.0,
                                    notes = "Torqued drain plug to 35 Nm"
                                )
                            )
                            database.maintenanceDao().insertRecord(
                                MaintenanceRecordEntity(
                                    id = "maint_02",
                                    vehicleId = defaultVehicleId,
                                    serviceDateUtcEpochMs = now - (90L * 24 * 3600 * 1000),
                                    odometerKm = 21000.0,
                                    category = "Brake Fluid Flush",
                                    workPerformed = "Castrol SRF high-temp racing fluid flush across all 4 Brembo calipers",
                                    partsReplaced = "Castrol SRF DOT4 Fluid",
                                    cost = 120.00,
                                    currency = "USD",
                                    nextDueDateUtcEpochMs = now + (270L * 24 * 3600 * 1000),
                                    nextDueKm = 40000.0,
                                    notes = "Zero pedal sponginess observed"
                                )
                            )
                            // Seed initial trip
                            database.tripDao().insertTrip(
                                TripEntity(
                                    id = "trip_01",
                                    vehicleId = defaultVehicleId,
                                    sessionId = "session_mountain_pass",
                                    startTimeUtcEpochMs = now - (3600 * 1000),
                                    endTimeUtcEpochMs = now - (1200 * 1000),
                                    distanceKm = 42.6,
                                    avgSpeedKmh = 64.2,
                                    maxRpm = 6800.0,
                                    sampleCount = 1840
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
