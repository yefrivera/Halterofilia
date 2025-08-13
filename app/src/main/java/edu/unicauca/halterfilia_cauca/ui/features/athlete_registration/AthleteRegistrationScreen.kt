package edu.unicauca.halterfilia_cauca.ui.features.athlete_registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteRegistrationScreen(
    navController: NavController,
    viewModel: AthleteRegistrationViewModel = viewModel()
) {
    val registrationState by viewModel.registrationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen for state changes to show snackbar or navigate back
    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegistrationState.Success -> {
                // Navigate back to the previous screen on success
                navController.popBackStack()
            }
            is RegistrationState.Error -> {
                // Show error message in a snackbar
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
                // Reset the state in the ViewModel so the error isn't shown again
                viewModel.resetRegistrationState()
            }
            else -> {
                // Idle or Loading, do nothing here
            }
        }
    }

    AthleteRegistrationContent(
        name = viewModel.name,
        birthDate = viewModel.birthDate,
        height = viewModel.height,
        weight = viewModel.weight,
        onNameChange = viewModel::onNameChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onSaveAthleteClicked = {
            viewModel.saveAthlete()
        },
        onNavigateBack = {
            navController.popBackStack()
        },
        isLoading = registrationState is RegistrationState.Loading,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteRegistrationContent(
    name: String,
    birthDate: String,
    height: String,
    weight: String,
    onNameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSaveAthleteClicked: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo Deportista") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isLoading) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nombres y apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = onBirthDateChange,
                label = { Text("Fecha de Nacimiento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Estatura (cm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Peso (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSaveAthleteClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Guardar Deportista")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            val formattedDate = sdf.format(Date(selectedDate))
                            onBirthDateChange(formattedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewAthleteScreenPreview() {
    HalterofiliaCaucaTheme {
        AthleteRegistrationContent(
            name = "Yefri Vera",
            birthDate = "01/01/1999",
            height = "180",
            weight = "80",
            onNameChange = {},
            onBirthDateChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onSaveAthleteClicked = {},
            onNavigateBack = {},
            isLoading = false,
            snackbarHostState = SnackbarHostState()
        )
    }
}
