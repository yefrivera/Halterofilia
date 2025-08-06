package edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.domain.model.BluetoothDeviceDomain
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme

@Composable
fun BluetoothScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel
) {
    val state by bluetoothViewModel.state.collectAsState()

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* Handle result */ }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_SCAN] == true
        } else {
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        if (hasPermission) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                bluetoothViewModel.startScan()
            }
        }
    }

    BluetoothContent(
        state = state,
        onStartScan = {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            permissionLauncher.launch(permissions)
        },
        onDeviceClick = { device ->
            if (!device.isConnected) {
                bluetoothViewModel.connectToDevice(device)
            }
        },
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothContent(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conectar Dispositivo BLE") },
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
                .padding(16.dp)
        ) {
            ConnectedDeviceStatus(
                device = state.connectedDevice
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartScan,
                    enabled = !state.isScanning
                ) {
                    Text(text = "Iniciar Escaneo")
                }
                if (state.isScanning) {
                    Spacer(modifier = Modifier.width(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            if (state.isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escaneando...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            BluetoothDeviceList(
                scannedDevices = state.scannedDevices,
                onDeviceClick = onDeviceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun ConnectedDeviceStatus(
    device: BluetoothDeviceDomain?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Dispositivo Conectado", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        DeviceStatusCard("Dispositivo", device)
    }
}

@Composable
fun DeviceStatusCard(deviceNameLabel: String, device: BluetoothDeviceDomain?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(deviceNameLabel, fontWeight = FontWeight.Bold)
                Text(
                    text = device?.name ?: "No conectado",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = device?.address ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val statusText = if (device != null) "Conectado" else "Desconectado"
            val statusColor = if (device != null) Color(0xFF4CAF50) else Color.Gray
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDeviceDomain>,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "Dispositivos Descubiertos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(scannedDevices) { device ->
            DeviceItem(
                device = device,
                onItemClick = { onDeviceClick(device) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDeviceDomain,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = device.name ?: "N/A", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onItemClick,
            enabled = !device.isConnected
        ) {
            Text(if (device.isConnected) "Conectado" else "Conectar")
        }
    }
}