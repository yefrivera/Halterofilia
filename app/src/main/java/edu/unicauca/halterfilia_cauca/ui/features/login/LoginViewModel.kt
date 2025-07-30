package edu.unicauca.halterfilia_cauca.ui.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginState())
    val uiState = _uiState.asStateFlow()

    fun onLoginEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }
            is LoginEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.password)
            }
            LoginEvent.Login -> {
                loginUser()
            }
            LoginEvent.MessageShown -> {
                _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
            }
        }
    }

    private fun loginUser() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, ingrese un correo válido.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, ingrese su contraseña.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Inicio de sesión exitoso.",
                            isLoginSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Error en el inicio de sesión: ${task.exception?.message}"
                        )
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object Login : LoginEvent()
    object MessageShown : LoginEvent()
}
