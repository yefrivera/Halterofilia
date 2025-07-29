package edu.unicauca.halterfilia_cauca.ui.features.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme
import java.time.Clock.offset


import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

/**
 * 1. Composable "Inteligente" (Stateful)
 * Obtiene el estado desde el ViewModel y lo pasa al Composable stateless.
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    registerViewModel: RegisterViewModel = viewModel()
) {
    RegisterContent(
        fullName = registerViewModel.fullName,
        email = registerViewModel.email,
        password = registerViewModel.password,
        onFullNameChange = { registerViewModel.onFullNameChange(it) },
        onEmailChange = { registerViewModel.onEmailChange(it) },
        onPasswordChange = { registerViewModel.onPasswordChange(it) },
        onRegisterClicked = {
            // Ejemplo: Navegar a la pantalla de login después del registro
            navController.navigate(AppScreens.LoginScreen.route)
        },
        onNavigateBack = {
            // Volver a la pantalla anterior
            navController.popBackStack()
        }
    )
}

/**
 * 2. Composable "Tonto" (Stateless)
 * Dibuja la UI basándose en los parámetros que recibe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    fullName: String,
    email: String,
    password: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            // Se reemplaza TopAppBar por la versión que centra el título
            CenterAlignedTopAppBar(
                title = { Text("Registro de usuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // Empuja el botón hacia abajo
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Campos de texto
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Nombres y apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Correo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Button(
                onClick = onRegisterClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp)
            ) {
                Text(text = "Registrarse", fontSize = 16.sp)
            }
        }
    }
}

/**
 * 3. Previsualización (Preview)
 * Llama al Composable stateless con datos de prueba.
 */
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    HalterofiliaCaucaTheme {
        RegisterContent(
            fullName = "Yefri Estiven Vera",
            email = "correo@ejemplo.com",
            password = "password123",
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRegisterClicked = {},
            onNavigateBack = {}
        )
    }
}