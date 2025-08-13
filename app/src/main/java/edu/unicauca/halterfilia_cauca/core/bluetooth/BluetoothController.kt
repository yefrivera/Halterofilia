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
import edu.unicauca.halterfilia_cauca.domain.model.BluetoothDeviceDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

sealed class BLEData {
    data class MeasurementData(val payload: String) : BLEData()
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

    // UUIDs del servicio y características
    private val serviceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val notifyCharacteristicUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // Para recibir datos (Notify)
    private val writeCharacteristicUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")  // Para enviar comandos (Write)
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // UUID del Descriptor CCCD

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> get() = _scannedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<Map<String, BluetoothDeviceDomain>>(emptyMap())
    val connectedDevices: StateFlow<Map<String, BluetoothDeviceDomain>> get() = _connectedDevices.asStateFlow()

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
                    Log.i("BLUETOOTH_DEBUG", "Device connected: $deviceAddress. Requesting MTU.")
                    _connectedDevices.update { devices ->
                        val device = gatt.device.toBluetoothDeviceDomain(context, isConnected = true)
                        devices + (deviceAddress to device)
                    }
                    gattConnections[deviceAddress] = gatt
                    gatt.requestMtu(517) // MTU alto para transferencia rápida
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLUETOOTH_DEBUG", "Device disconnected: $deviceAddress")
                    _connectedDevices.update { devices -> devices - deviceAddress }
                    gattConnections.remove(deviceAddress)?.close()
                }
            } else {
                Log.e("BLUETOOTH_DEBUG", "Connection error for $deviceAddress: status=$status")
                _connectedDevices.update { devices -> devices - deviceAddress }
                gattConnections.remove(deviceAddress)?.close()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLUETOOTH_DEBUG", "MTU changed to $mtu")
                gatt?.discoverServices()
            } else {
                Log.w("BLUETOOTH_DEBUG", "MTU change failed. Status: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLUETOOTH_DEBUG", "Services discovered for ${gatt?.device?.address}")
                val targetService = gatt?.getService(serviceUUID)
                if (targetService == null) {
                    Log.e("BLUETOOTH_DEBUG", "Service with UUID $serviceUUID NOT FOUND.")
                    return
                }

                val notifyCharacteristic = targetService.getCharacteristic(notifyCharacteristicUUID)
                if (notifyCharacteristic != null) {
                    Log.i("BLUETOOTH_DEBUG", "Found Notify Characteristic. Enabling notifications.")
                    enableNotifications(gatt, notifyCharacteristic)
                } else {
                    Log.e("BLUETOOTH_DEBUG", "Notify Characteristic with UUID $notifyCharacteristicUUID NOT FOUND.")
                }

                if (targetService.getCharacteristic(writeCharacteristicUUID) == null) {
                    Log.e("BLUETOOTH_DEBUG", "Write Characteristic with UUID $writeCharacteristicUUID NOT FOUND.")
                } else {
                    Log.i("BLUETOOTH_DEBUG", "Found Write Characteristic.")
                }
            } else {
                Log.w("BLUETOOTH_DEBUG", "onServicesDiscovered received error: $status")
            }
        }

        // ✅ **INICIO DEL CÓDIGO CORREGIDO**

        // Callback para Android 13 (API 33) y superior.
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == notifyCharacteristicUUID) {
                handleReceivedData(value)
            }
        }

        // Callback para Android 12 (API 32) e inferiores. Esencial para compatibilidad.
        @Deprecated("Used for Android 12 and below")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == notifyCharacteristicUUID) {
                @Suppress("DEPRECATION")
                val value = characteristic.value
                handleReceivedData(value)
            }
        }

        // ✅ **FIN DEL CÓDIGO CORREGIDO**

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLUETOOTH_DEBUG", "Descriptor written successfully. Notifications enabled.")
            } else {
                Log.e("BLUETOOTH_DEBUG", "Failed to write descriptor. Status: $status")
            }
        }
    }

    // ✅ **NUEVA FUNCIÓN PARA CENTRALIZAR LA LÓGICA**
    private fun handleReceivedData(value: ByteArray) {
        val receivedString = String(value, Charsets.UTF_8)
        Log.d("BLE_RECEIVE", "Datos recibidos: '$receivedString'")

        // Comprueba si la cadena recibida contiene el marcador de finalización "END"
        if (receivedString.contains("END")) {
            val dataBeforeEnd = receivedString.substringBefore("END")
            if (dataBeforeEnd.isNotEmpty()) {
                dataBuffer.append(dataBeforeEnd)
                Log.i("BLUETOOTH_DEBUG", "Chunk recibido: $dataBeforeEnd. Tamaño del buffer: ${dataBuffer.length}")
            }

            Log.i("BLUETOOTH_DEBUG", ">>>>>> FIN DE DATOS RECIBIDO <<<<<<")
            _bleData.value = BLEData.MeasurementData(dataBuffer.toString())
            dataBuffer.clear() // Reinicia el buffer para el próximo mensaje
        } else {
            // Si no hay marcador de finalización, simplemente añade los datos al buffer
            dataBuffer.append(receivedString)
            Log.i("BLUETOOTH_DEBUG", "Chunk recibido: $receivedString. Tamaño del buffer: ${dataBuffer.length}")
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor == null) {
            Log.e("BLUETOOTH_DEBUG", "Descriptor CCCD no encontrado en la característica ${characteristic.uuid}")
            return
        }

        // ✅ **VERIFICACIÓN DE VERSIÓN PARA COMPATIBILIDAD**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    fun sendData(address: String, data: String) {
        val gatt = gattConnections[address]
        val characteristic = gatt?.getService(serviceUUID)?.getCharacteristic(writeCharacteristicUUID)

        if (gatt == null || characteristic == null) {
            Log.e("BLUETOOTH_DEBUG", "No se pueden enviar datos. Dispositivo no conectado o característica no encontrada.")
            return
        }

        if (data == "START" || data == "STOP") {
            dataBuffer.clear()
            _bleData.value = null
        }

        val value = data.toByteArray(Charsets.UTF_8)

        // ✅ **VERIFICACIÓN DE VERSIÓN PARA COMPATIBILIDAD**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
        Log.i("BLUETOOTH_DEBUG", "Enviando datos: '$data' a $address.")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name != null) {
                _scannedDevices.update { devices ->
                    val newDevice = result.device.toBluetoothDeviceDomain(context)
                    if (newDevice !in devices) devices + newDevice else devices
                }
            }
        }
    }

    fun startScan() {
        if (!hasScanPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Permiso de escaneo no concedido.")
            return
        }
        if (_isScanning.value) return
        if (bleScanner == null) {
            Log.e("BLUETOOTH_DEBUG", "BluetoothLeScanner no disponible.")
            return
        }

        Log.i("BLUETOOTH_DEBUG", "Iniciando escaneo BLE...")
        _scannedDevices.update { emptyList() }
        _isScanning.value = true
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bleScanner?.startScan(null, scanSettings, scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            if (_isScanning.value) stopScan()
        }, 15000)
    }

    fun stopScan() {
        if (!hasScanPermission()) return
        if (!_isScanning.value) return

        Log.i("BLUETOOTH_DEBUG", "Deteniendo escaneo BLE.")
        _isScanning.value = false
        bleScanner?.stopScan(scanCallback)
    }

    fun connectToDevice(address: String) {
        if (!hasConnectPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Permiso de conexión no concedido.")
            return
        }
        if (gattConnections.containsKey(address)) return

        Log.i("BLUETOOTH_DEBUG", "Intentando conectar al dispositivo: $address")
        bluetoothAdapter?.getRemoteDevice(address)?.connectGatt(context, false, gattCallback)
    }

    fun disconnectDevice(address: String) {
        if (!hasConnectPermission()) {
            Log.w("BLUETOOTH_DEBUG", "Permiso de conexión no concedido para desconectar.")
            return
        }
        Log.i("BLUETOOTH_DEBUG", "Desconectando del dispositivo: $address")
        gattConnections[address]?.disconnect()
    }

    fun release() {
        Log.i("BLUETOOTH_DEBUG", "Liberando todos los recursos de Bluetooth.")
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

private fun BluetoothDevice.toBluetoothDeviceDomain(context: Context, isConnected: Boolean = false): BluetoothDeviceDomain {
    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            name
        } else {
            "Unknown"
        }
    } else {
        name
    }
    return BluetoothDeviceDomain(
        name = deviceName,
        address = address,
        isConnected = isConnected
    )
}