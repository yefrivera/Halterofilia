package edu.unicauca.halterfilia_cauca.ui.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.R
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme

import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

@Composable
fun LoginScreen(navController: NavController, loginViewModel: LoginViewModel = viewModel()) {
    LoginContent(
        email = loginViewModel.email,
        password = loginViewModel.password,
        onEmailChange = { loginViewModel.onEmailChange(it) },
        onPasswordChange = { loginViewModel.onPasswordChange(it) },
        onLoginClicked = {
            // Ejemplo: Navegar a la pantalla de atleta al iniciar sesión
            navController.navigate(AppScreens.AthleteScreen.route)
        },
        onRegisterClicked = {
            // Navegar a la pantalla de registro
            navController.navigate(AppScreens.RegisterScreen.route)
        }
    )
}

@Composable
fun LoginContent(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de la pantalla
        Text(
            text = "Liga Caucana de Halterofilia",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Logo de la aplicación
        Image(
            painter = painterResource(id = R.drawable.liga),
            contentDescription = "Logo de la Liga",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de texto para el Correo
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para la Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón para Iniciar sesión
        Button(
            onClick = onLoginClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Iniciar sesión", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Texto para la opción de registro
        Text(text = "¿No tienes una cuenta?")

        Spacer(modifier = Modifier.height(8.dp))

        // Botón para Registrarse
        Button(
            onClick = onRegisterClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(text = "Regístrate", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HalterofiliaCaucaTheme {
    LoginContent(
        email = "correo@ejemplo.com",
        password = "password123",
        onEmailChange = {},
        onPasswordChange = {},
        onLoginClicked = {},
        onRegisterClicked = {}
    )
     }
}