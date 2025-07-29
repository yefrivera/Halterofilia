package edu.unicauca.halterfilia_cauca.ui.features.measurement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// 1. Modelo de datos para una sola repetición
data class Repetition(
    val id: Int,
    val name: String,
    val angle: Float? = null // El ángulo puede ser nulo al principio
)

// 2. ViewModel para la pantalla de medición
class MeasureViewModel : ViewModel() {

    // Nombre del deportista (en una app real, vendría de la pantalla anterior)
    var athleteName by mutableStateOf("Deportista X")
        private set

    // Estado para saber si la medición está activa
    var isMeasuring by mutableStateOf(false)
        private set

    // Lista de repeticiones. Se irá llenando dinámicamente.
    var repetitions by mutableStateOf(listOf<Repetition>())
        private set

    // --- Acciones del Usuario ---

    fun onStartMeasuring() {
        isMeasuring = true
        // TODO: Lógica para empezar a recibir datos del sensor
        // Simulación: agregamos una repetición cada vez que se inicia
        val newRepId = (repetitions.size + 1)
        repetitions = repetitions + Repetition(id = newRepId, name = "Repetición $newRepId")
    }

    fun onStopMeasuring() {
        isMeasuring = false
        // TODO: Lógica para detener la recepción de datos
    }

    fun onRepetitionOptionsClicked(repetition: Repetition) {
        // TODO: Lógica para mostrar opciones de una repetición (ej. eliminar, ver detalles)
    }
}