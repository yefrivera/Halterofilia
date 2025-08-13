package edu.unicauca.halterfilia_cauca.domain.model

/**
 * Represents a single data point received from an ESP32 device.
 * This structure mirrors the binary format sent over BLE.
 */
data class DataPoint(
    // Propiedades que ya estabas usando en tu mapeo
    val id: Int = 0,
    val idx: Int = 0,
    val source: String = "",
    val angle: Float = 0.0f,
    val time: Long = 0L
)
