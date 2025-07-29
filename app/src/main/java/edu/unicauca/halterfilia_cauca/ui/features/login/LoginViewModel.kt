package edu.unicauca.halterfilia_cauca.ui.features.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    // Estado para el campo de correo electrónico
    var email by mutableStateOf("")
        private set

    // Estado para el campo de contraseña
    var password by mutableStateOf("")
        private set

    // Función para actualizar el correo electrónico desde la UI
    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    // Función para actualizar la contraseña desde la UI
    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    // Aquí iría la lógica para iniciar sesión, pero por ahora está vacía
    fun onLoginClicked() {
        // Lógica de inicio de sesión (no implementada)
    }

    // Aquí iría la lógica para registrarse, pero por ahora está vacía
    fun onRegisterClicked() {
        // Lógica de registro (no implementada)
    }
}