package edu.unicauca.halterfilia_cauca.ui.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.R
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

@Composable
fun LoginScreen(navController: NavController, loginViewModel: LoginViewModel = viewModel()) {
    val uiState by loginViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Efecto para navegar cuando el inicio de sesión sea exitoso
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            snackbarHostState.showSnackbar("Inicio de sesión exitoso")
            navController.navigate(AppScreens.AthleteScreen.route) {
                popUpTo(AppScreens.LoginScreen.route) { inclusive = true }
            }
            loginViewModel.onLoginEvent(LoginEvent.MessageShown)
        }
    }

    // Efecto para mostrar mensajes de error
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            loginViewModel.onLoginEvent(LoginEvent.MessageShown)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LoginContent(
            uiState = uiState,
            onEvent = loginViewModel::onLoginEvent,
            onRegisterClicked = {
                navController.navigate(AppScreens.RegisterScreen.route)
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun LoginContent(
    uiState: LoginState,
    onEvent: (LoginEvent) -> Unit,
    onRegisterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
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
                isError = uiState.errorMessage != null
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
                isError = uiState.errorMessage != null
            )

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
