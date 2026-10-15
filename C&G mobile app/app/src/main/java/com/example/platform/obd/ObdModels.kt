package com.example.platform.obd

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.VehicleSample

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STALE_DATA,
    PERMISSION_DENIED,
    UNSUPPORTED_HARDWARE,
    ERROR
}

enum class AdapterTransport {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    CNG_IOT_CUSTOM,
    SIMULATOR
}

data class AdapterDevice(
    val address: String,
    val name: String,
    val isBonded: Boolean = true,
    val transport: AdapterTransport = AdapterTransport.BLUETOOTH_CLASSIC
)

data class AdapterCapabilities(
    val elmVersion: String,
    val detectedProtocol: String,
    val supportedPidsCount: Int,
    val voltage: String
)

data class ObdCommand(
    val command: String,
    val description: String = "",
    val timeoutMs: Long = 3000L
)

data class ObdResponse(
    val raw: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val dataBytes: List<Int> = emptyList()
)

data class PidDefinition(
    val pid: String, // e.g. "010C"
    val mode: String, // e.g. "01"
    val pidHex: String, // e.g. "0C"
    val name: String,
    val shortName: String,
    val unit: String,
    val minValue: Double,
    val maxValue: Double,
    val bytesExpected: Int,
    val decode: (List<Int>) -> Double?
)
