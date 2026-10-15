package com.example.platform.obd

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.DtcType
import com.example.domain.models.SampleValidity
import com.example.domain.models.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class DrivingProfile {
    IDLE,
    CITY_CRUISE,
    HIGHWAY_SPRINT,
    OVERHEATING_FAULT,
    MISFIRE_FAULT
}

class FakeObdAdapter(
    var drivingProfile: DrivingProfile = DrivingProfile.CITY_CRUISE
) : ObdAdapter {
    override val adapterId: String = "fake_simulator_01"
    override val displayName: String = "C&G Virtual Telemetry Simulator"
    override val transport: AdapterTransport = AdapterTransport.SIMULATOR

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: ConnectionState get() = _connectionState.value
    override val connectionStateFlow: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<VehicleSample>(extraBufferCapacity = 64)
    override val telemetryFlow: Flow<VehicleSample> = _telemetryFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var telemetryJob: Job? = null
    private var tick = 0

    override suspend fun discoverDevices(): List<AdapterDevice> {
        return listOf(
            AdapterDevice("SIM:00:11:22", "C&G Virtual Dyno Rig", isBonded = true, transport = AdapterTransport.SIMULATOR),
            AdapterDevice("SIM:33:44:55", "Stress Test Simulator (Fault Injected)", isBonded = true, transport = AdapterTransport.SIMULATOR)
        )
    }

    override suspend fun connect(device: AdapterDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        delay(600) // realistic handshake latency
        _connectionState.value = ConnectionState.CONNECTED
        startTelemetryLoop()
    }

    override suspend fun disconnect() {
        telemetryJob?.cancel()
        telemetryJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun initialize(): AdapterCapabilities {
        delay(400)
        return AdapterCapabilities(
            elmVersion = "ELM327 v1.5 (Simulated)",
            detectedProtocol = "ISO 15765-4 (CAN 11/500)",
            supportedPidsCount = 28,
            voltage = "14.2V"
        )
    }

    override suspend fun discoverSupportedPids(): Set<String> {
        delay(300)
        return setOf(
            PidRegistry.PID_ENGINE_RPM,
            PidRegistry.PID_VEHICLE_SPEED,
            PidRegistry.PID_COOLANT_TEMP,
            PidRegistry.PID_ENGINE_LOAD,
            PidRegistry.PID_INTAKE_TEMP,
            PidRegistry.PID_THROTTLE_POS,
            PidRegistry.PID_MAP,
            PidRegistry.PID_MAF,
            PidRegistry.PID_MODULE_VOLTAGE,
            PidRegistry.PID_FUEL_LEVEL,
            PidRegistry.PID_RUN_TIME
        )
    }

    override suspend fun executeReadCommand(command: ObdCommand): ObdResponse {
        delay(40)
        val cmd = command.command.uppercase().trim()
        val resp = when {
            cmd == "ATZ" -> "ELM327 v1.5"
            cmd == "ATE0" -> "OK"
            cmd == "ATL0" -> "OK"
            cmd == "ATH0" -> "OK"
            cmd == "ATSP0" -> "OK"
            cmd == "0100" -> "41 00 BE 3E B8 11" // bitmap for PIDs 01-20
            cmd == "0120" -> "41 20 80 00 00 01" // bitmap for PIDs 21-40
            cmd == "010C" -> {
                // RPM
                val rpm = currentRpm()
                val rawVal = (rpm * 4).toInt()
                val a = (rawVal shr 8) and 0xFF
                val b = rawVal and 0xFF
                String.format("41 0C %02X %02X", a, b)
            }
            cmd == "010D" -> {
                // Speed
                val spd = currentSpeed().toInt()
                String.format("41 0D %02X", spd)
            }
            cmd == "0105" -> {
                // Coolant temp
                val temp = currentCoolantTemp().toInt() + 40
                String.format("41 05 %02X", temp.coerceIn(0, 255))
            }
            cmd == "0104" -> {
                // Load
                val load = (currentLoad() * 255.0 / 100.0).toInt()
                String.format("41 04 %02X", load.coerceIn(0, 255))
            }
            cmd == "03" -> {
                // Trouble codes
                "43 01 15 03 00 00 00" // P0115, P0300
            }
            cmd == "04" -> "44" // clear DTC
            else -> "NO DATA"
        }
        return ObdResponse(raw = resp, isSuccess = true)
    }

    override suspend fun readTroubleCodes(): List<DiagnosticCode> {
        delay(500)
        return when (drivingProfile) {
            DrivingProfile.OVERHEATING_FAULT -> listOf(
                DiagnosticCode(
                    code = "P0115",
                    category = "Powertrain",
                    description = "Engine Coolant Temperature Circuit Malfunction",
                    codeType = DtcType.STORED,
                    severity = DtcSeverity.HIGH,
                    possibleCauses = listOf("Faulty ECT sensor", "Wiring short to ground", "Low coolant level"),
                    recommendedActions = listOf("Check coolant reservoir", "Inspect ECT harness connector", "Consult technician")
                )
            )
            DrivingProfile.MISFIRE_FAULT -> listOf(
                DiagnosticCode(
                    code = "P0300",
                    category = "Powertrain",
                    description = "Random / Multiple Cylinder Misfire Detected",
                    codeType = DtcType.STORED,
                    severity = DtcSeverity.HIGH,
                    possibleCauses = listOf("Worn spark plugs", "Failing ignition coil", "Fuel pressure drop"),
                    recommendedActions = listOf("Check spark plug electrode wear", "Perform coil pack resistance check")
                )
            )
            else -> emptyList()
        }
    }

    override suspend fun clearTroubleCodes(): Boolean {
        delay(600)
        return true
    }

    override fun dispose() {
        telemetryJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                tick++
                val now = System.currentTimeMillis()

                val rpmVal = currentRpm()
                val speedVal = currentSpeed()
                val coolantVal = currentCoolantTemp()
                val loadVal = currentLoad()
                val throttleVal = currentThrottle()
                val voltageVal = 14.1 + (sin(tick * 0.1) * 0.2)
                val intakeVal = 28.0 + (sin(tick * 0.05) * 4.0)

                emitSample(PidRegistry.PID_ENGINE_RPM, "Engine RPM", rpmVal, "rpm", now)
                emitSample(PidRegistry.PID_VEHICLE_SPEED, "Vehicle Speed", speedVal, "km/h", now)
                emitSample(PidRegistry.PID_COOLANT_TEMP, "Coolant Temperature", coolantVal, "°C", now)
                emitSample(PidRegistry.PID_ENGINE_LOAD, "Calculated Engine Load", loadVal, "%", now)
                emitSample(PidRegistry.PID_THROTTLE_POS, "Throttle Position", throttleVal, "%", now)
                emitSample(PidRegistry.PID_MODULE_VOLTAGE, "Module Voltage", voltageVal, "V", now)
                emitSample(PidRegistry.PID_INTAKE_TEMP, "Intake Air Temperature", intakeVal, "°C", now)

                delay(200) // 5 Hz update rate
            }
        }
    }

    private suspend fun emitSample(pid: String, name: String, value: Double, unit: String, timestamp: Long) {
        _telemetryFlow.emit(
            VehicleSample(
                vehicleId = "active_vehicle",
                sessionId = "sim_session",
                parameterId = pid,
                parameterName = name,
                value = Math.round(value * 10.0) / 10.0,
                unit = unit,
                capturedAtUtcEpochMs = timestamp,
                receivedAtUtcEpochMs = timestamp,
                sourceAdapter = displayName,
                validity = SampleValidity.VALID
            )
        )
    }

    private fun currentRpm(): Double {
        return when (drivingProfile) {
            DrivingProfile.IDLE -> 750.0 + (sin(tick * 0.2) * 35.0) + Random.nextDouble(-10.0, 10.0)
            DrivingProfile.CITY_CRUISE -> 1800.0 + (sin(tick * 0.15) * 850.0) + Random.nextDouble(-30.0, 30.0)
            DrivingProfile.HIGHWAY_SPRINT -> 3200.0 + (sin(tick * 0.1) * 1400.0) + Random.nextDouble(-50.0, 50.0)
            DrivingProfile.OVERHEATING_FAULT -> 2100.0 + (sin(tick * 0.15) * 400.0)
            DrivingProfile.MISFIRE_FAULT -> 1600.0 + (sin(tick * 0.3) * 600.0) + Random.nextDouble(-120.0, 120.0)
        }.coerceIn(0.0, 7500.0)
    }

    private fun currentSpeed(): Double {
        return when (drivingProfile) {
            DrivingProfile.IDLE -> 0.0
            DrivingProfile.CITY_CRUISE -> (45.0 + (sin(tick * 0.1) * 25.0)).coerceAtLeast(0.0)
            DrivingProfile.HIGHWAY_SPRINT -> (105.0 + (sin(tick * 0.08) * 20.0)).coerceAtLeast(0.0)
            DrivingProfile.OVERHEATING_FAULT -> (55.0 + (sin(tick * 0.1) * 15.0)).coerceAtLeast(0.0)
            DrivingProfile.MISFIRE_FAULT -> (38.0 + (sin(tick * 0.12) * 18.0)).coerceAtLeast(0.0)
        }
    }

    private fun currentCoolantTemp(): Double {
        return when (drivingProfile) {
            DrivingProfile.OVERHEATING_FAULT -> (102.0 + (tick * 0.2).coerceAtMost(16.0)) // climbs to 118°C!
            else -> 88.0 + (sin(tick * 0.04) * 4.0)
        }
    }

    private fun currentLoad(): Double {
        return when (drivingProfile) {
            DrivingProfile.IDLE -> 18.0 + (sin(tick * 0.1) * 4.0)
            DrivingProfile.HIGHWAY_SPRINT -> 68.0 + (sin(tick * 0.1) * 22.0)
            else -> 38.0 + (sin(tick * 0.12) * 18.0)
        }.coerceIn(0.0, 100.0)
    }

    private fun currentThrottle(): Double {
        return (currentLoad() * 0.85 + (sin(tick * 0.1) * 5.0)).coerceIn(0.0, 100.0)
    }
}
