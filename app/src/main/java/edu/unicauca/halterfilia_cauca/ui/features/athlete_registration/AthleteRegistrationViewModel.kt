package edu.unicauca.halterfilia_cauca.ui.features.athlete_registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class NewAthleteViewModel : ViewModel() {

    var fullName by mutableStateOf("")
        private set

    var age by mutableStateOf("")
        private set

    var height by mutableStateOf("")
        private set

    var weight by mutableStateOf("")
        private set

    fun onFullNameChange(newName: String) {
        fullName = newName
    }

    fun onAgeChange(newAge: String) {
        age = newAge
    }

    fun onHeightChange(newHeight: String) {
        height = newHeight
    }

    fun onWeightChange(newWeight: String) {
        weight = newWeight
    }

    fun onAddAthleteClicked() {
        // TODO: Lógica para guardar el nuevo deportista en la base de datos
    }
}