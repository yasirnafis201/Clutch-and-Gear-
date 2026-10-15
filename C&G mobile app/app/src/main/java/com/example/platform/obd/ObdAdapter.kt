package com.example.platform.obd

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.VehicleSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ObdAdapter {
    val adapterId: String
    val displayName: String
    val transport: AdapterTransport
    val connectionState: ConnectionState

    val connectionStateFlow: StateFlow<ConnectionState>
    val telemetryFlow: Flow<VehicleSample>

    suspend fun discoverDevices(): List<AdapterDevice>
    suspend fun connect(device: AdapterDevice)
    suspend fun disconnect()

    suspend fun initialize(): AdapterCapabilities
    suspend fun discoverSupportedPids(): Set<String>

    suspend fun executeReadCommand(command: ObdCommand): ObdResponse
    suspend fun readTroubleCodes(): List<DiagnosticCode>
    suspend fun clearTroubleCodes(): Boolean

    fun dispose()
}
