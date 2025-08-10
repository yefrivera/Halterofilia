package edu.unicauca.halterfilia_cauca.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import edu.unicauca.halterfilia_cauca.domain.model.BluetoothDeviceDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

sealed class BLEData {
    data class MeasurementData(val payload: String) : BLEData()
    data class SlaveStatus(val status: String) : BLEData()
}


@SuppressLint("MissingPermission")
class BluetoothController(
    private val context: Context
) {
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    // Correct Service and Characteristic UUIDs from the .ino file
    private val serviceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val notifyCharacteristicUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // For receiving data
    private val writeCharacteristicUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")  // For sending commands

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<Map<String, BluetoothDeviceDomain>>(emptyMap())
    val connectedDevices: StateFlow<Map<String, BluetoothDeviceDomain>>
        get() = _connectedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bleData = MutableStateFlow<BLEData?>(null)
    val bleData: StateFlow<BLEData?> = _bleData.asStateFlow()

    private val gattConnections = mutableMapOf<String, BluetoothGatt>()
    private val dataBuffer = StringBuilder()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address ?: return
            Log.d("BLUETOOTH_DEBUG", "onConnectionStateChange for $deviceAddress: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLUETOOTH_DEBUG", "Device connected: $deviceAddress. Starting service discovery.")
                    _connectedDevices.update { devices ->
                        val device = gatt.device.toBluetoothDeviceDomain(isConnected = true)
                        devices + (deviceAddress to device)
                    }
                    gattConnections[deviceAddress] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLUETOOTH_DEBUG", "Device disconnected: $deviceAddress")
                    _connectedDevices.update { devices ->
                        devices - deviceAddress
                    }
                    gattConnections.remove(deviceAddress)?.close()
                }
            } else {
                Log.e("BLUETOOTH_DEBUG", "Connection error for $deviceAddress: status=$status")
                _connectedDevices.update { devices ->
                    devices - deviceAddress
                }
                gattConnections.remove(deviceAddress)?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLUETOOTH_DEBUG", "Services discovered successfully for ${gatt?.device?.address}")
                val targetService = gatt?.getService(serviceUUID)
                if (targetService == null) {
                    Log.e("BLUETOOTH_DEBUG", "Service with UUID $serviceUUID NOT FOUND.")
                    return
                }

                val notifyCharacteristic = targetService.getCharacteristic(notifyCharacteristicUUID)
                if (notifyCharacteristic == null) {
                    Log.e("BLUETOOTH_DEBUG", "Notify Characteristic with UUID $notifyCharacteristicUUID NOT FOUND.")
                } else {
                    Log.i("BLUETOOTH_DEBUG", "Found Notify Characteristic. Enabling notifications.")
                    enableNotifications(gatt, notifyCharacteristic)
                }

                val writeCharacteristic = targetService.getCharacteristic(writeCharacteristicUUID)
                if (writeCharacteristic == null) {
                    Log.e("BLUETOOTH_DEBUG", "Write Characteristic with UUID $writeCharacteristicUUID NOT FOUND.")
                } else {
                    Log.i("BLUETOOTH_DEBUG", "Found Write Characteristic.")
                }

            } else {
                Log.w("BLUETOOTH_DEBUG", "onServicesDiscovered received error status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val receivedString = String(value, Charsets.UTF_8)
            // AÑADE ESTA LÍNEA PARA VER TODO LO QUE LLEGA
            Log.d("BLE_RECEIVE", "Datos recibidos del Master: '$receivedString'")
            if (characteristic.uuid == notifyCharacteristicUUID) {

                when (receivedString) {
                    "SLAVE_OK", "SLAVE_ERROR" -> {
                        _bleData.value = BLEData.SlaveStatus(receivedString)
                    }
                    "END" -> {
                        Log.i("BLUETOOTH_DEBUG", ">>>>>> END OF DATA RECEIVED <<<<<<")
                        _bleData.value = BLEData.MeasurementData(dataBuffer.toString())
                        dataBuffer.clear() // Reset buffer for next message
                    }
                    else -> {
                        dataBuffer.append(receivedString)
                        Log.i("BLUETOOTH_DEBUG", "Received chunk: $receivedString. Buffer size: ${dataBuffer.length}")
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLUETOOTH_DEBUG", "Descriptor written successfully. Notifications enabled.")
            } else {
                Log.e("BLUETOOTH_DEBUG", "Failed to write descriptor. Status: $status")
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor == null) {
            Log.e("BLUETOOTH_DEBUG", "CCCD descriptor not found for characteristic ${characteristic.uuid}")
            return
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        gatt.writeDescriptor(descriptor)
    }

    fun sendData(address: String, data: String) {
        val gatt = gattConnections[address]
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(writeCharacteristicUUID)

        if (gatt == null || characteristic == null) {
            Log.e("BLUETOOTH_DEBUG", "Cannot send data. Device not connected or characteristic not found.")
            return
        }

        if (data == "START" || data == "STOP" || data == "CHECK_SLAVE") {
            dataBuffer.clear()
            _bleData.value = null
        }

        val value = data.toByteArray(Charsets.UTF_8)
        characteristic.setValue(value)
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val success = gatt.writeCharacteristic(characteristic)
        Log.i("BLUETOOTH_DEBUG", "Sending data: '$data' to $address. Success: $success")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null) {
                _scannedDevices.update { devices ->
                    val newDevice = device.toBluetoothDeviceDomain()
                    if (newDevice in devices) devices else devices + newDevice
                }
            }
        }
    }

    fun startScan() {
        if (!hasScanPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Scan permission not granted.")
            return
        }
        if (_isScanning.value) {
            return
        }
        if (bleScanner == null) {
            Log.e("BLUETOOTH_DEBUG", "BluetoothLeScanner not available. Check if Bluetooth is enabled.")
            return
        }

        Log.i("BLUETOOTH_DEBUG", "Starting BLE scan...")
        _scannedDevices.update { emptyList() }
        _isScanning.value = true
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(null, scanSettings, scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            if (_isScanning.value) {
                stopScan()
            }
        }, 15000)
    }

    fun stopScan() {
        if (!hasScanPermission()) {
            return
        }
        if (!_isScanning.value) {
            return
        }
        Log.i("BLUETOOTH_DEBUG", "Stopping BLE scan.")
        _isScanning.value = false
        bleScanner?.stopScan(scanCallback)
    }

    fun connectToDevice(address: String) {
        if (!hasConnectPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Connect permission not granted.")
            return
        }
        if (gattConnections.containsKey(address)) {
            return
        }
        Log.i("BLUETOOTH_DEBUG", "Attempting to connect to device: $address")
        val device = bluetoothAdapter?.getRemoteDevice(address)
        device?.connectGatt(context, false, gattCallback)
    }

    fun disconnectDevice(address: String) {
        if (!hasConnectPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Connect permission not granted to disconnect.")
            return
        }
        Log.i("BLUETOOTH_DEBUG", "Disconnecting from device: $address")
        gattConnections[address]?.disconnect()
    }

    fun release() {
        Log.i("BLUETOOTH_DEBUG", "Releasing all Bluetooth resources.")
        gattConnections.values.forEach { it.close() }
        gattConnections.clear()
        if (isScanning.value) {
            stopScan()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun BluetoothDevice.toBluetoothDeviceDomain(isConnected: Boolean = false): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isConnected = isConnected
    )
}
