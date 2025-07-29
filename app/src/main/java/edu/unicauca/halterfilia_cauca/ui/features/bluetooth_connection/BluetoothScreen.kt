package edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme


import androidx.navigation.NavController

/**
 * Composable Stateful (inteligente)
 */
@Composable
fun BluetoothScreen(
    navController: NavController,
    connectDeviceViewModel: ConnectDeviceViewModel = viewModel()
) {
    ConnectDeviceContent(
        devices = connectDeviceViewModel.availableDevices.value,
        onDeviceSelected = {
            // Ejemplo: Volver a la pantalla anterior al seleccionar un dispositivo
            navController.popBackStack()
        },
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}


/**
 * Composable Stateless (tonto) que dibuja la UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDeviceContent(
    devices: List<BluetoothDeviceModel>,
    onDeviceSelected: (BluetoothDeviceModel) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conectar Dispositivo") },
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
            Text(
                text = "Dispositivos Bluetooth disponibles",
                style = MaterialTheme.typography.titleMedium
            )

            // Contenedor para la lista
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                LazyColumn {
                    items(devices) { device ->
                        DeviceItem(
                            deviceName = device.name,
                            onItemClick = { onDeviceSelected(device) }
                        )
                        // No agregar divisor para el último elemento
                        if (device != devices.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable para un solo dispositivo en la lista
 */
@Composable
fun DeviceItem(
    deviceName: String,
    onItemClick: () -> Unit
) {
    Text(
        text = deviceName,
        fontSize = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp)
    )
}


/**
 * Previsualización
 */
@Preview(showBackground = true)
@Composable
fun ConnectDeviceScreenPreview() {
    HalterofiliaCaucaTheme {
        ConnectDeviceContent(
            devices = listOf(
                BluetoothDeviceModel("Dispositivo 1", "00:00:00:00:00:01"),
                BluetoothDeviceModel("Dispositivo 2", "00:00:00:00:00:02"),
                BluetoothDeviceModel("Dispositivo 3", "00:00:00:00:00:03")
            ),
            onDeviceSelected = {},
            onNavigateBack = {}
        )
    }
}