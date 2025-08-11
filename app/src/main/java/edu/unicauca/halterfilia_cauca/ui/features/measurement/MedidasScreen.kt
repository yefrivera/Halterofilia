package edu.unicauca.halterfilia_cauca.ui.features.measurement

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController
import edu.unicauca.halterfilia_cauca.data.FileRepository
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository

@Composable
fun MedidasScreen(
    navController: NavController,
    athleteName: String,
    athleteDocId: String,
    bluetoothController: BluetoothController
) {
    val context = LocalContext.current
    val measurementRepository = MeasurementRepository()
    val fileRepository = FileRepository(context)
    val medidasViewModel: MedidasViewModel = viewModel(
        factory = MedidasViewModelFactory(bluetoothController, measurementRepository, fileRepository, athleteDocId)
    )
    val state by medidasViewModel.state.collectAsState()

    LaunchedEffect(state.lastReceivedData) {
        if (state.lastReceivedData.isNotEmpty()) {
            Toast.makeText(context, "Datos recibidos y guardados con éxito", Toast.LENGTH_SHORT).show()
        }
    }

    if (state.lastMeasurementJson != null) {
        AlertDialog(
            onDismissRequest = { medidasViewModel.clearLastMeasurementJson() },
            title = { Text("Última Medición") },
            text = { Text(state.lastMeasurementJson!!) },
            confirmButton = {
                Button(onClick = { medidasViewModel.clearLastMeasurementJson() }) {
                    Text("Cerrar")
                }
            }
        )
    }

    MedidasContent(
        athleteName = athleteName,
        state = state,
        onStartClicked = { medidasViewModel.startMeasurement() },
        onStopClicked = { medidasViewModel.stopMeasurement() },
        onSaveClicked = { medidasViewModel.saveMeasurement() },
        onNavigateBack = { navController.popBackStack() },
        onShowLastMeasurement = { medidasViewModel.showLastMeasurement() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedidasContent(
    athleteName: String,
    state: MeasurementState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowLastMeasurement: () -> Unit
) {
    Scaffold(
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
            // Master Connection Status
            Text(
                text = if (state.isConnected) "Dispositivo Conectado" else "Dispositivo Desconectado",
                color = if (state.isConnected) Color(0xFF00C853) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (state.isReceiving) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Recibiendo datos...", style = MaterialTheme.typography.bodyLarge)
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (state.isMeasuring) onStopClicked() else onStartClicked()
                    },
                    enabled = state.isConnected && !state.isReceiving, // Deshabilitar mientras se reciben datos
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isMeasuring) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (state.isMeasuring) "Detener Medición" else "Iniciar Medición")
                }
                if (!state.isMeasuring && state.calculatedValue != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onSaveClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar Medida")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShowLastMeasurement,
            ) {
                Text("Ver Última Medición")
            }
        }
    }
}
