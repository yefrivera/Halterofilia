package edu.unicauca.halterfilia_cauca.ui.features.athlete_registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.unicauca.halterfilia_cauca.domain.model.Athlete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegistrationState {
    object Idle : RegistrationState
    object Loading : RegistrationState
    object Success : RegistrationState
    data class Error(val message: String) : RegistrationState
}

class AthleteRegistrationViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Form fields
    var name by mutableStateOf("")
        private set
    var birthDate by mutableStateOf("")
        private set
    var height by mutableStateOf("")
        private set
    var weight by mutableStateOf("")
        private set

    // UI State
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState = _registrationState.asStateFlow()

    fun onNameChange(newName: String) {
        name = newName
    }

    fun onBirthDateChange(newBirthDate: String) {
        birthDate = newBirthDate
    }

    fun onHeightChange(newHeight: String) {
        height = newHeight
    }

    fun onWeightChange(newWeight: String) {
        weight = newWeight
    }

    fun saveAthlete() {
        if (name.isBlank() || birthDate.isBlank() || height.isBlank() || weight.isBlank()) {
            _registrationState.value = RegistrationState.Error("Todos los campos son obligatorios.")
            return
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _registrationState.value = RegistrationState.Error("No se pudo obtener el usuario actual.")
                return@launch
            }

            val newAthlete = Athlete(
                name = name,
                birthDate = birthDate,
                height = height,
                weight = weight
            )

            firestore.collection("users").document(userId).collection("athletes")
                .add(newAthlete)
                .addOnSuccessListener {
                    _registrationState.value = RegistrationState.Success
                }
                .addOnFailureListener { e ->
                    _registrationState.value = RegistrationState.Error(e.message ?: "Error al guardar el deportista.")
                }
        }
    }

    fun resetRegistrationState() {
        _registrationState.value = RegistrationState.Idle
    }
}
