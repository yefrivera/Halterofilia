package edu.unicauca.halterfilia_cauca.domain.model

/**
 * Represents a single data point received from an ESP32 device.
 * This structure mirrors the binary format sent over BLE.
 */
data class DataPoint(
    val id: Int = 0,      // Device ID (0 for Master, 1 for Slave)
    val idx: Int = 0,     // Packet index
    val timeMs: Long = 0L, // Timestamp in milliseconds from the start of measurement
    val angle: Float = 0f, // Angle value
    val source: String = "" // "Master" or "Slave", derived from id
)
