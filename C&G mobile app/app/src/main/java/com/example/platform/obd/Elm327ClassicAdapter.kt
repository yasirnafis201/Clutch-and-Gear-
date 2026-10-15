package com.example.platform.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.domain.models.DiagnosticCode
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
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class Elm327ClassicAdapter(
    private val context: Context
) : ObdAdapter {
    override val adapterId: String = "elm327_bt_classic"
    override val displayName: String = "Generic ELM327 Bluetooth Classic"
    override val transport: AdapterTransport = AdapterTransport.BLUETOOTH_CLASSIC

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: ConnectionState get() = _connectionState.value
    override val connectionStateFlow: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<VehicleSample>(extraBufferCapacity = 64)
    override val telemetryFlow: Flow<VehicleSample> = _telemetryFlow.asSharedFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pollingJob: Job? = null
    private val supportedPids = mutableSetOf<String>()

    private val commandQueue = ObdCommandQueue { cmd ->
        sendRawCommand(cmd)
    }

    override suspend fun discoverDevices(): List<AdapterDevice> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext emptyList()
        try {
            val paired = adapter.bondedDevices ?: emptySet()
            paired.map { device: BluetoothDevice ->
                AdapterDevice(
                    address = device.address,
                    name = device.name ?: "Unknown OBD Device",
                    isBonded = true,
                    transport = AdapterTransport.BLUETOOTH_CLASSIC
                )
            }
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.PERMISSION_DENIED
            emptyList()
        }
    }

    override suspend fun connect(device: AdapterDevice): Unit = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.CONNECTING
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            _connectionState.value = ConnectionState.UNSUPPORTED_HARDWARE
            return@withContext
        }

        try {
            val btDevice = adapter.getRemoteDevice(device.address)
            adapter.cancelDiscovery()

            val socket = btDevice.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()

            bluetoothSocket = socket
            inputStream = socket.inputStream
            outputStream = socket.outputStream

            // Initialize adapter
            initialize()
            discoverSupportedPids()

            _connectionState.value = ConnectionState.CONNECTED
            startPollingLoop()
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.PERMISSION_DENIED
            disconnect()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            disconnect()
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        pollingJob?.cancel()
        pollingJob = null
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        bluetoothSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun initialize(): AdapterCapabilities = withContext(Dispatchers.IO) {
        // Standard AT initialization sequence
        executeReadCommand(ObdCommand("ATZ", "Reset", 2000))
        delay(100)
        executeReadCommand(ObdCommand("ATE0", "Echo Off", 1000))
        executeReadCommand(ObdCommand("ATL0", "Linefeeds Off", 1000))
        executeReadCommand(ObdCommand("ATS0", "Spaces Off", 1000))
        executeReadCommand(ObdCommand("ATH0", "Headers Off", 1000))
        executeReadCommand(ObdCommand("ATSP0", "Auto Protocol", 3000))

        val voltResp = executeReadCommand(ObdCommand("ATRV", "Read Voltage", 1000))
        val protoResp = executeReadCommand(ObdCommand("ATDP", "Read Protocol", 1000))

        AdapterCapabilities(
            elmVersion = "ELM327",
            detectedProtocol = Elm327Parser.cleanResponse(protoResp.raw).ifEmpty { "Auto (CAN)" },
            supportedPidsCount = supportedPids.size,
            voltage = Elm327Parser.cleanResponse(voltResp.raw).ifEmpty { "12.6V" }
        )
    }

    override suspend fun discoverSupportedPids(): Set<String> = withContext(Dispatchers.IO) {
        supportedPids.clear()
        // Query 0100
        val resp0100 = executeReadCommand(ObdCommand("0100", "PIDs 01-20", 2000))
        val bytes0100 = Elm327Parser.parseMode01DataBytes(resp0100.raw, "00")
        supportedPids.addAll(PidRegistry.parseSupportedBitmap(bytes0100, baseRange = 0x00))

        // Check if range 0120 is supported
        if (PidRegistry.hasNextRange(bytes0100)) {
            val resp0120 = executeReadCommand(ObdCommand("0120", "PIDs 21-40", 2000))
            val bytes0120 = Elm327Parser.parseMode01DataBytes(resp0120.raw, "20")
            supportedPids.addAll(PidRegistry.parseSupportedBitmap(bytes0120, baseRange = 0x20))
        }

        supportedPids
    }

    override suspend fun executeReadCommand(command: ObdCommand): ObdResponse {
        return commandQueue.execute(command)
    }

    override suspend fun readTroubleCodes(): List<DiagnosticCode> = withContext(Dispatchers.IO) {
        val resp = executeReadCommand(ObdCommand("03", "Read Stored DTCs", 4000))
        Elm327Parser.parseDiagnosticTroubleCodes(resp.raw, DtcType.STORED)
    }

    override suspend fun clearTroubleCodes(): Boolean = withContext(Dispatchers.IO) {
        val resp = executeReadCommand(ObdCommand("04", "Clear DTCs", 5000))
        resp.isSuccess
    }

    override fun dispose() {
        scope.launch { disconnect() }
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            val activePids = listOf(
                PidRegistry.PID_ENGINE_RPM,
                PidRegistry.PID_VEHICLE_SPEED,
                PidRegistry.PID_COOLANT_TEMP,
                PidRegistry.PID_ENGINE_LOAD,
                PidRegistry.PID_MODULE_VOLTAGE
            )

            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                for (pid in activePids) {
                    if (!isActive) break
                    val def = PidRegistry.ALL_DEFINITIONS[pid] ?: continue
                    val resp = executeReadCommand(ObdCommand(pid, def.name, 1500))
                    if (resp.isSuccess) {
                        val bytes = Elm327Parser.parseMode01DataBytes(resp.raw, def.pidHex)
                        val decodedVal = def.decode(bytes)
                        if (decodedVal != null) {
                            val now = System.currentTimeMillis()
                            _telemetryFlow.emit(
                                VehicleSample(
                                    vehicleId = "active_vehicle",
                                    sessionId = "session_live",
                                    parameterId = pid,
                                    parameterName = def.name,
                                    value = Math.round(decodedVal * 10.0) / 10.0,
                                    unit = def.unit,
                                    capturedAtUtcEpochMs = now,
                                    receivedAtUtcEpochMs = now,
                                    sourceAdapter = displayName,
                                    validity = SampleValidity.VALID
                                )
                            )
                        }
                    }
                    delay(50) // Inter-command pacing for ELM327 stability
                }
                delay(100)
            }
        }
    }

    private suspend fun sendRawCommand(cmd: String): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw IllegalStateException("Adapter not connected")
        val input = inputStream ?: throw IllegalStateException("Adapter not connected")

        // Flush remaining buffer
        while (input.available() > 0) {
            input.read()
        }

        val bytesToSend = "$cmd\r".toByteArray(Charsets.US_ASCII)
        out.write(bytesToSend)
        out.flush()

        val sb = StringBuilder()
        val buffer = ByteArray(256)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < 3000) {
            if (input.available() > 0) {
                val readCount = input.read(buffer)
                if (readCount > 0) {
                    val chunk = String(buffer, 0, readCount, Charsets.US_ASCII)
                    sb.append(chunk)
                    if (chunk.contains(">")) {
                        break
                    }
                }
            } else {
                delay(20)
            }
        }

        sb.toString()
    }
}
