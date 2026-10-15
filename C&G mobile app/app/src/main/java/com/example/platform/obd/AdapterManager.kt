package com.example.platform.obd

import android.content.Context
import com.example.domain.models.DiagnosticCode
import com.example.domain.models.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdapterManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    val fakeAdapter = FakeObdAdapter()
    val classicAdapter = Elm327ClassicAdapter(context)
    val iotAdapter = CngIotAdapter()

    private var activeAdapter: ObdAdapter = fakeAdapter

    private val _connectionState = MutableStateFlow(activeAdapter.connectionState)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryFlow = MutableSharedFlow<VehicleSample>(extraBufferCapacity = 64)
    val telemetryFlow: Flow<VehicleSample> = _telemetryFlow.asSharedFlow()

    private val _activeDevice = MutableStateFlow<AdapterDevice?>(null)
    val activeDevice: StateFlow<AdapterDevice?> = _activeDevice.asStateFlow()

    private var adapterCollectionJob: Job? = null

    init {
        bindAdapter(fakeAdapter)
    }

    fun getActiveAdapter(): ObdAdapter = activeAdapter

    fun selectAdapterType(transport: AdapterTransport) {
        scope.launch {
            activeAdapter.disconnect()
            activeAdapter = when (transport) {
                AdapterTransport.BLUETOOTH_CLASSIC -> classicAdapter
                AdapterTransport.CNG_IOT_CUSTOM -> iotAdapter
                else -> fakeAdapter
            }
            bindAdapter(activeAdapter)
        }
    }

    private fun bindAdapter(adapter: ObdAdapter) {
        adapterCollectionJob?.cancel()
        adapterCollectionJob = scope.launch {
            launch {
                adapter.connectionStateFlow.collect { state ->
                    _connectionState.value = state
                }
            }
            launch {
                adapter.telemetryFlow.collect { sample ->
                    _telemetryFlow.emit(sample)
                }
            }
        }
    }

    suspend fun discoverDevices(): List<AdapterDevice> {
        return activeAdapter.discoverDevices()
    }

    suspend fun connect(device: AdapterDevice) {
        _activeDevice.value = device
        activeAdapter.connect(device)
    }

    suspend fun disconnect() {
        activeAdapter.disconnect()
        _activeDevice.value = null
    }

    suspend fun readDtcCodes(): List<DiagnosticCode> {
        return activeAdapter.readTroubleCodes()
    }

    suspend fun clearDtcCodes(): Boolean {
        return activeAdapter.clearTroubleCodes()
    }
}
