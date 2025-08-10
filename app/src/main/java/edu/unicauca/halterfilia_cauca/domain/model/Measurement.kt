package edu.unicauca.halterfilia_cauca.domain.model

data class Measurement(
    val value: Double,
    val timestamp: Long,
    val athleteId: String
)
