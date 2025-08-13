package edu.unicauca.halterfilia_cauca.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una única sesión de medición completa para un atleta.
 * Este es el objeto que se guardará como un documento en Firestore.
 */
data class MeasurementSession(
    @DocumentId
    val id: String? = null, // Firestore poblará este campo con el ID del documento
    val userId: String = "", // ID del usuario que realiza la medición
    val athleteId: String = "",
    val userEmail: String = "", // Email del usuario que realiza la medición
    val timestamp: Date? = null, // Firestore asignará la fecha del servidor
    val dataPoints: List<DataPoint> = emptyList() // Lista de todos los puntos de datos de la sesión
)
