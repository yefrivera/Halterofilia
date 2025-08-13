package edu.unicauca.halterfilia_cauca.ui.features.measurement

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens
import kotlinx.coroutines.launch

@Composable
fun MedidasScreen(
    navController: NavController,
    athleteName: String,
    athleteId: String,
    medidasViewModel: MedidasViewModel
) {
    val state by medidasViewModel.state.collectAsState()

    MedidasContent(
        athleteName = athleteName,
        state = state,
        onStartClicked = { medidasViewModel.startMeasurement() },
        onStopClicked = { medidasViewModel.stopMeasurement() },
        onNavigateBack = { navController.popBackStack() },
        resetSaveStatus = { medidasViewModel.resetSaveStatus() },
        onHistoryClicked = {
            navController.navigate(AppScreens.HistoryScreen.route + "/$athleteName/$athleteId")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedidasContent(
    athleteName: String,
    state: MeasurementState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onNavigateBack: () -> Unit,
    resetSaveStatus: () -> Unit,
    onHistoryClicked: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saveStatus) {
        if (state.saveStatus == SaveStatus.SUCCESS) {
            scope.launch {
                snackbarHostState.showSnackbar("Medidas tomadas correctamente")
                resetSaveStatus() // Resetea el estado para no volver a mostrarlo
            }
        }
        if (state.saveStatus == SaveStatus.ERROR) {
            scope.launch {
                snackbarHostState.showSnackbar("Error al guardar las medidas")
                resetSaveStatus()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Deportista: $athleteName") },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (state.isConnected) "Dispositivo Conectado" else "Dispositivo Desconectado",
                color = if (state.isConnected) Color(0xFF00C853) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isMeasuring || state.isStopping || state.saveStatus == SaveStatus.SAVING) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when {
                        state.isStopping -> "Deteniendo..."
                        state.isMeasuring -> "Midiendo..."
                        else -> "Guardando..."
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                // Contenedor con altura fija para evitar que los botones salten
                Box(modifier = Modifier.height(80.dp)) {
                    // No se muestra nada aquí, pero mantiene el espacio
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onStartClicked,
                    enabled = state.isConnected && !state.isMeasuring,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Iniciar Medición")
                }

                Button(
                    onClick = onStopClicked,
                    enabled = state.isMeasuring && !state.isStopping,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Parar Medición")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onHistoryClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Historial")
            }
        }
    }
}