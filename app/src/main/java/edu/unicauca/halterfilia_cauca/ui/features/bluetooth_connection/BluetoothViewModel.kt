package edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

// 1. Modelo de datos para un dispositivo Bluetooth
data class BluetoothDeviceModel(
    val name: String,
    val address: String // La dirección MAC es útil en una app real
)

// 2. ViewModel para manejar el estado
class ConnectDeviceViewModel : ViewModel() {

    // Lista de ejemplo de dispositivos encontrados
    val availableDevices = mutableStateOf(
        listOf(
            BluetoothDeviceModel(name = "Dispositivo 1", address = "00:11:22:AA:BB:CC"),
            BluetoothDeviceModel(name = "Dispositivo 2", address = "DD:EE:FF:33:44:55"),
            BluetoothDeviceModel(name = "Dispositivo 3", address = "GG:HH:II:66:77:88")
        )
    )

    // Acciones del usuario (la lógica se implementaría después)
    fun onDeviceSelected(device: BluetoothDeviceModel) {
        // TODO: Lógica para intentar conectar al dispositivo
    }
}