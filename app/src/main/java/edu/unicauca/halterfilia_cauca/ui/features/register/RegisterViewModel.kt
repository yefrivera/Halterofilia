package edu.unicauca.halterfilia_cauca.ui.features.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(RegisterState())
    val uiState = _uiState.asStateFlow()

    fun onRegisterEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }
            is RegisterEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.password)
            }
            is RegisterEvent.ConfirmPasswordChanged -> {
                _uiState.value = _uiState.value.copy(confirmPassword = event.confirmPassword)
            }
            RegisterEvent.Register -> {
                registerUser()
            }
            RegisterEvent.MessageShown -> {
                _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
            }
        }
    }

    private fun registerUser() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, ingrese un correo válido.")
            return
        }

        if (password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, complete todos los campos.")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Las contraseñas no coinciden.")
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Registro exitoso.",
                            isRegistrationSuccess = true
                        )
                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "El correo electrónico ya está en uso."
                            else -> "Error en el registro: ${task.exception?.message}"
                        }
                        _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }
}

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRegistrationSuccess: Boolean = false
)

sealed class RegisterEvent {
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    object Register : RegisterEvent()
    object MessageShown : RegisterEvent()
}
