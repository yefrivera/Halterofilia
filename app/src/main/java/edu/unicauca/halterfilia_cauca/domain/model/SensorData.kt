package edu.unicauca.halterfilia_cauca.domain.model

data class SensorData(
    val ax_slave: Float = 0f,
    val ay_slave: Float = 0f,
    val az_slave: Float = 0f,
    val gx_slave: Float = 0f,
    val gy_slave: Float = 0f,
    val gz_slave: Float = 0f,
    val roll_master: Float = 0f,
    val pitch_master: Float = 0f,
    val error: String? = null
)
