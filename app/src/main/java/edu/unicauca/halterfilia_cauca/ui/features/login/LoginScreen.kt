package edu.unicauca.halterfilia_cauca.ui.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import edu.unicauca.halterfilia_cauca.R
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme

@Composable
fun LoginScreen(navController: NavController, loginViewModel: LoginViewModel = viewModel()) {
    val uiState by loginViewModel.uiState.collectAsState()

    // Efecto para navegar cuando el inicio de sesión sea exitoso
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            navController.navigate(AppScreens.AthleteScreen.route) {
                // Limpia el backstack para que el usuario no pueda volver a la pantalla de login
                popUpTo(AppScreens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    // Efecto para mostrar mensajes (Snackbar, Toast, etc.)
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Aquí puedes mostrar un Snackbar o un Toast
            // Por simplicidad, lo dejaremos como un comentario.
            // scaffoldState.snackbarHostState.showSnackbar(message)
            loginViewModel.onLoginEvent(LoginEvent.MessageShown)
        }
    }

    LoginContent(
        uiState = uiState,
        onEvent = loginViewModel::onLoginEvent,
        onRegisterClicked = {
            navController.navigate(AppScreens.RegisterScreen.route)
        }
    )
}

@Composable
fun LoginContent(
    uiState: LoginState,
    onEvent: (LoginEvent) -> Unit,
    onRegisterClicked: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Liga Caucana de Halterofilia",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.liga),
                contentDescription = "Logo de la Liga",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = uiState.errorMessage?.contains("correo") == true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = uiState.errorMessage?.contains("contraseña") == true
            )

            Spacer(modifier = Modifier.height(8.dp))

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onEvent(LoginEvent.Login) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(text = "Iniciar sesión", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "¿No tienes una cuenta?")

            Spacer(modifier = Modifier.height(8.dp))

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

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HalterofiliaCaucaTheme {
        LoginContent(
            uiState = LoginState(email = "correo@ejemplo.com", password = "password123"),
            onEvent = {},
            onRegisterClicked = {}
        )
    }
}
