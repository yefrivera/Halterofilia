package edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController
import edu.unicauca.halterfilia_cauca.domain.model.BluetoothDeviceDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val connectedDevice: BluetoothDeviceDomain? = null,
    val isScanning: Boolean = false
)

class BluetoothViewModel(
    app: Application
) : AndroidViewModel(app) {

    val bluetoothController by lazy {
        BluetoothController(app)
    }

    val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.connectedDevices,
        bluetoothController.isScanning,
        _state
    ) { scannedDevices, connectedDevices, isScanning, state ->
        // Find the first connected device
        val connectedDevice = connectedDevices.values.firstOrNull()

        // Update scanned devices with their connection status
        val updatedScannedDevices = scannedDevices.map { scannedDevice ->
            val isConnected = connectedDevices.containsKey(scannedDevice.address)
            scannedDevice.copy(isConnected = isConnected)
        }

        state.copy(
            scannedDevices = updatedScannedDevices,
            connectedDevice = connectedDevice,
            isScanning = isScanning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun startScan() {
        Log.d("BluetoothViewModel", "Start Scan called")
        bluetoothController.startScan()
    }

    fun stopScan() {
        Log.d("BluetoothViewModel", "Stop Scan called")
        bluetoothController.stopScan()
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        // Allow connecting only if there is no device connected
        if (state.value.connectedDevice == null) {
            Log.d("BluetoothViewModel", "Connecting to device: ${device.address}")
            bluetoothController.connectToDevice(device.address)
        } else {
            Log.d("BluetoothViewModel", "Cannot connect, a device is already connected.")
        }
    }

    fun disconnectDevice(device: BluetoothDeviceDomain) {
        Log.d("BluetoothViewModel", "Disconnecting from device: ${device.address}")
        bluetoothController.disconnectDevice(device.address)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}
