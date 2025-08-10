package edu.unicauca.halterfilia_cauca.ui.features.athlete

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.core.utils.DateUtils
import edu.unicauca.halterfilia_cauca.domain.model.Athlete
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

enum class AthleteMenuAction {
    CONSULT, MEASURE, HISTORY, DELETE
}

@Composable
fun AthleteScreen(
    navController: NavController,
    athleteViewModel: AthleteViewModel = viewModel()
) {
    val uiState by athleteViewModel.uiState.collectAsState()

    val onAddAthleteClicked = { navController.navigate(AppScreens.AthleteRegistrationScreen.route) }
    val onBluetoothClicked = { navController.navigate(AppScreens.BluetoothScreen.route) }
    val onLogoutClicked = { athleteViewModel.onLogoutClicked() }

    val onAthleteOptionSelected = { athlete: Athlete, action: AthleteMenuAction ->
        when (action) {
            AthleteMenuAction.CONSULT -> athleteViewModel.onConsultAthleteClicked(athlete)
            AthleteMenuAction.MEASURE -> {
                val athleteId = athlete.id ?: ""
                navController.navigate(
                    AppScreens.MedidasScreen.route + "/${athlete.name}/$athleteId"
                )
            }
            AthleteMenuAction.HISTORY -> navController.navigate(AppScreens.HistoryScreen.route)
            AthleteMenuAction.DELETE -> athleteViewModel.onDeleteAthleteClicked(athlete)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAthleteClicked) {
                Icon(Icons.Default.Add, contentDescription = "Agregar deportista")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AthleteUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AthleteUiState.Empty -> {
                EmptyStateContent(
                    onAddAthleteClicked = onAddAthleteClicked,
                    onLogoutClicked = onLogoutClicked,
                    onBluetoothClicked = onBluetoothClicked,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AthleteUiState.Success -> {
                AthletesListContent(
                    athletes = state.athletes,
                    onLogoutClicked = onLogoutClicked,
                    onBluetoothClicked = onBluetoothClicked,
                    onAthleteOptionSelected = onAthleteOptionSelected,
                    modifier = Modifier.padding(paddingValues)
                )

                if (state.showConsultDialog && state.selectedAthlete != null) {
                    ConsultEditAthleteDialog(
                        athlete = state.selectedAthlete,
                        onDismiss = { athleteViewModel.dismissConsultDialog() },
                        onSave = { updatedAthlete ->
                            athleteViewModel.onUpdateAthlete(updatedAthlete)
                        }
                    )
                }

                if (state.showDeleteConfirmation && state.selectedAthlete != null) {
                    DeleteConfirmationDialog(
                        athleteName = state.selectedAthlete.name,
                        onConfirm = { athleteViewModel.confirmAthleteDeletion() },
                        onDismiss = { athleteViewModel.dismissDeleteConfirmation() }
                    )
                }

                if (state.showLogoutConfirmation) {
                    LogoutConfirmationDialog(
                        onConfirm = {
                            athleteViewModel.confirmLogout {
                                navController.navigate(AppScreens.LoginScreen.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        },
                        onDismiss = { athleteViewModel.dismissLogoutConfirmation() }
                    )
                }
            }
            is AthleteUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AthletesListContent(
    athletes: List<Athlete>,
    onLogoutClicked: () -> Unit,
    onBluetoothClicked: () -> Unit,
    onAthleteOptionSelected: (Athlete, AthleteMenuAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            onLogoutClicked = onLogoutClicked,
            onBluetoothClicked = onBluetoothClicked
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(athletes) { athlete ->
                AthleteItem(
                    athlete = athlete,
                    onOptionSelected = { action -> onAthleteOptionSelected(athlete, action) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmptyStateContent(
    onAddAthleteClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onBluetoothClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            onLogoutClicked = onLogoutClicked,
            onBluetoothClicked = onBluetoothClicked
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido Entrenador",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aún no hay deportistas agregados. ¿Deseas agregar uno?",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddAthleteClicked) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Agregar Deportista")
            }
        }
    }
}

@Composable
fun ScreenHeader(
    onLogoutClicked: () -> Unit,
    onBluetoothClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Deportistas", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Row {
            IconButton(onClick = onBluetoothClicked) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Conectar dispositivo bluetooth", modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onLogoutClicked) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun AthleteItem(
    athlete: Athlete,
    onOptionSelected: (AthleteMenuAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Icono de deportista",
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = athlete.name,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp
        )
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Consultar datos") }, onClick = { onOptionSelected(AthleteMenuAction.CONSULT); expanded = false })
                DropdownMenuItem(text = { Text("Tomar medidas") }, onClick = { onOptionSelected(AthleteMenuAction.MEASURE); expanded = false })
                DropdownMenuItem(text = { Text("Ver historial") }, onClick = { onOptionSelected(AthleteMenuAction.HISTORY); expanded = false })
                DropdownMenuItem(text = { Text("Eliminar deportista") }, onClick = { onOptionSelected(AthleteMenuAction.DELETE); expanded = false })
            }
        }
    }
}

@Composable
fun ConsultEditAthleteDialog(
    athlete: Athlete,
    onDismiss: () -> Unit,
    onSave: (Athlete) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(athlete.name) }
    var birthDate by remember { mutableStateOf(athlete.birthDate) }
    var height by remember { mutableStateOf(athlete.height) }
    var weight by remember { mutableStateOf(athlete.weight) }

    val age = remember(birthDate) { DateUtils.calculateAge(birthDate)?.toString() ?: "N/A" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Editar Deportista" else "Consultar Deportista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, readOnly = !isEditMode)
                OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Fecha de Nacimiento (dd/MM/yyyy)") }, readOnly = !isEditMode)
                if (!isEditMode) {
                    Text("Edad: $age años")
                }
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Talla (cm)") }, readOnly = !isEditMode)
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (kg)") }, readOnly = !isEditMode)
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(onClick = { showSaveConfirmation = true }) {
                    Text("Guardar")
                }
            } else {
                Button(onClick = { isEditMode = true }) {
                    Text("Editar")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Confirmar Cambios") },
            text = { Text("¿Estás seguro de que deseas guardar los cambios?") },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedAthlete = athlete.copy(name = name, birthDate = birthDate, height = height, weight = weight)
                        onSave(updatedAthlete)
                        showSaveConfirmation = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                Button(onClick = { showSaveConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    athleteName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Deportista") },
        text = { Text("¿Estás seguro de que deseas eliminar a $athleteName? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar Sesión") },
        text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
