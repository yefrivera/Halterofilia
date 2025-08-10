package edu.unicauca.halterfilia_cauca.domain.model

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false
)
