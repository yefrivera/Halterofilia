package edu.unicauca.halterfilia_cauca.ui.features.register

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = uiState.isRegistrationSuccess) {
        if (uiState.isRegistrationSuccess) {
            navController.navigate(AppScreens.AthleteScreen.route) {
                popUpTo(AppScreens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(key1 = uiState.errorMessage) {
        uiState.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.onRegisterEvent(RegisterEvent.MessageShown)
            }
        }
    }

    LaunchedEffect(key1 = uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.onRegisterEvent(RegisterEvent.MessageShown)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("Registro de Usuario", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onRegisterEvent(RegisterEvent.EmailChanged(it)) },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage?.contains("correo") == true
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onRegisterEvent(RegisterEvent.PasswordChanged(it)) },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage?.contains("contraseña") == true
                )

                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { viewModel.onRegisterEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                    label = { Text("Confirmar Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage?.contains("contraseñas no coinciden") == true
                )

                Button(
                    onClick = { viewModel.onRegisterEvent(RegisterEvent.Register) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Registrarse")
                    }
                }

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("¿Ya tienes una cuenta? Inicia Sesión")
                }
            }
        }
    }
}
