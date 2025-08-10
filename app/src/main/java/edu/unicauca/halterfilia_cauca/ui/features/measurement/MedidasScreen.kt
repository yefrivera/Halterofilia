package edu.unicauca.halterfilia_cauca.ui.features.measurement

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository

@Composable
fun MedidasScreen(
    navController: NavController,
    athleteName: String,
    athleteDocId: String,
    bluetoothController: BluetoothController
) {
    val measurementRepository = MeasurementRepository()
    val medidasViewModel: MedidasViewModel = viewModel(
        factory = MedidasViewModelFactory(bluetoothController, measurementRepository, athleteDocId)
    )
    val state by medidasViewModel.state.collectAsState()

    MedidasContent(
        athleteName = athleteName,
        state = state,
        onStartClicked = { medidasViewModel.startMeasurement() },
        onStopClicked = { medidasViewModel.stopMeasurement() },
        onSaveClicked = { medidasViewModel.saveMeasurement() },
        onNavigateBack = { navController.popBackStack() },
        onCheckSlave = { medidasViewModel.checkSlaveStatus() }
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
    onCheckSlave: () -> Unit
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
            Spacer(modifier = Modifier.height(8.dp))

            // Slave Connection Status
            val slaveStatusText: String
            val slaveStatusColor: Color
            when (state.slaveStatus) {
                SlaveStatus.UNKNOWN -> {
                    slaveStatusText = "Esclavo: Desconocido"
                    slaveStatusColor = Color.Gray
                }
                SlaveStatus.CHECKING -> {
                    slaveStatusText = "Verificando Esclavo..."
                    slaveStatusColor = Color.Blue
                }
                SlaveStatus.CONNECTED -> {
                    slaveStatusText = "Esclavo: Conectado"
                    slaveStatusColor = Color(0xFF00C853)
                }
                SlaveStatus.ERROR -> {
                    slaveStatusText = "Esclavo: No encontrado"
                    slaveStatusColor = Color.Red
                }
            }
            Text(
                text = slaveStatusText,
                color = slaveStatusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display Measurement Result
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (state.isMeasuring) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = state.calculatedValue?.let { String.format("%.2f", it) } ?: "0.00",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Último dato recibido: ${state.lastReceivedData}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (state.isMeasuring) onStopClicked() else onStartClicked()
                    },
                    enabled = state.isConnected && state.slaveStatus == SlaveStatus.CONNECTED,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isMeasuring) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (state.isMeasuring) "Detener Medición" else "Iniciar Medición")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onSaveClicked,
                    enabled = state.calculatedValue != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar Medida")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCheckSlave,
                enabled = state.isConnected
            ) {
                Text("Verificar Esclavo")
            }
        }
    }
}
