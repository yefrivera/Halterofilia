package edu.unicauca.halterfilia_cauca.ui.features.athlete

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

// 1. Modelo de datos para representar a un deportista
data class Athlete(
    val id: Int,
    val name: String
)

// 2. ViewModel para manejar la lógica y el estado de la pantalla
class AthletesViewModel : ViewModel() {

    // Estado que contiene la lista de deportistas.
    // Usamos datos de ejemplo para el diseño.
    val athletes = mutableStateOf(
        listOf(
            Athlete(id = 1, name = "Deportista 1"),
            Athlete(id = 2, name = "Deportista 2"),
            Athlete(id = 3, name = "Deportista 3"),
            Athlete(id = 4, name = "Deportista 4"),
        )
    )

    // Funciones para los eventos de la UI (por ahora vacías)
    fun onAddAthleteClicked() { /* TODO: Lógica para agregar */ }
    fun onConnectBluetoothClicked() { /* TODO: Lógica para conectar BT */ }
    fun onLogoutClicked() { /* TODO: Lógica para cerrar sesión */ }
    fun onAthleteOptionsClicked(athlete: Athlete) { /* TODO: Lógica para mostrar opciones */ }
}