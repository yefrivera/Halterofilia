package edu.unicauca.halterfilia_cauca.ui.features.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// 1. Modelo de datos para un item del historial
data class RepetitionHistoryItem(
    val id: Int,
    val name: String
)

// 2. ViewModel para la pantalla de historial
class HistoryViewModel : ViewModel() {

    // --- ESTADOS DE LA UI ---
    var selectedDate by mutableStateOf(LocalDate.now()) // Fecha seleccionada, inicia en hoy
        private set

    var repetitionsForDate by mutableStateOf(listOf<RepetitionHistoryItem>())
        private set

    var isLoading by mutableStateOf(false) // Para mostrar un indicador de carga
        private set

    var showCalendar by mutableStateOf(false) // Para mostrar/ocultar el calendario
        private set

    // --- BASE DE DATOS FALSA (PARA EJEMPLO) ---
    // En una app real, esto sería una consulta a tu base de datos (Room, Firebase, etc.)
    private val fakeDatabase = mapOf(
        LocalDate.now() to listOf(
            RepetitionHistoryItem(1, "Repetición 1"),
            RepetitionHistoryItem(2, "Repetición 2"),
            RepetitionHistoryItem(3, "Repetición 3")
        ),
        LocalDate.now().minusDays(1) to listOf(
            RepetitionHistoryItem(4, "Repetición A"),
            RepetitionHistoryItem(5, "Repetición B")
        ),
        LocalDate.now().minusDays(2) to emptyList() // Un día sin datos
    )

    init {
        // Carga los datos para la fecha actual al iniciar
        fetchRepetitionsForDate(selectedDate)
    }

    // --- ACCIONES DEL USUARIO ---
    fun onDateSelected(dateInMillis: Long?) {
        dateInMillis?.let {
            val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            selectedDate = newDate
            fetchRepetitionsForDate(newDate)
        }
        showCalendar = false
    }

    fun onShowCalendar() {
        showCalendar = true
    }

    fun onDismissCalendar() {
        showCalendar = false
    }

    private fun fetchRepetitionsForDate(date: LocalDate) {
        isLoading = true
        // Simulación de la llamada a la base de datos
        val results = fakeDatabase[date] ?: emptyList()
        repetitionsForDate = results
        isLoading = false
    }

    fun onRepetitionClicked(item: RepetitionHistoryItem) {
        // TODO: Aquí irá la lógica para navegar a la pantalla de detalles de esta repetición
        println("Clicked on: ${item.name}")
    }
}