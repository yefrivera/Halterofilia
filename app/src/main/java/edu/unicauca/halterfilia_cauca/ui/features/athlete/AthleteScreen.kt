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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme


import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

/**
 * Composable "Inteligente" (Stateful)
 */
@Composable
fun AthletesScreen(
    navController: NavController,
    athletesViewModel: AthletesViewModel = viewModel()
) {
    AthletesContent(
        athletes = athletesViewModel.athletes.value,
        onAddAthleteClicked = {
            // Navegar a la pantalla de registro de atleta
            navController.navigate(AppScreens.AthleteRegistrationScreen.route)
        },
        onConnectBluetoothClicked = {
            // Navegar a la pantalla de conexión Bluetooth
            navController.navigate(AppScreens.BluetoothScreen.route)
        },
        onLogoutClicked = {
            // Navegar a la pantalla de login y limpiar el backstack
            navController.navigate(AppScreens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        },
        onAthleteOptionsClicked = {
            // Ejemplo: Navegar a la pantalla de historial del atleta
            navController.navigate(AppScreens.HistoryScreen.route)
        }
    )
}

/**
 * Composable "Tonto" (Stateless) que dibuja la UI.
 */
@Composable
fun AthletesContent(
    athletes: List<Athlete>,
    onAddAthleteClicked: () -> Unit,
    onConnectBluetoothClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onAthleteOptionsClicked: (Athlete) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Cabecera con título y botón de salir
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Deportistas", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onLogoutClicked) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Lista de deportistas (scrollable)
        LazyColumn(
            modifier = Modifier.weight(1f) // Ocupa todo el espacio disponible
        ) {
            items(athletes) { athlete ->
                AthleteItem(
                    athlete = athlete,
                    onOptionsClicked = { onAthleteOptionsClicked(athlete) }
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(44.dp))

        // 3. Botones inferiores
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp)
            ) {
            Button(
                onClick = onAddAthleteClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Agregar deportista", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConnectBluetoothClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Conectar dispositivo Bluetooth", fontSize = 16.sp)
            }
        }
    }
}

/**
 * Composable para un solo item de la lista de deportistas.
 */
@Composable
fun AthleteItem(
    athlete: Athlete,
    onOptionsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Icono de deportista",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = athlete.name,
            modifier = Modifier.weight(1f), // Empuja el siguiente icono a la derecha
            fontSize = 18.sp
        )
        IconButton(onClick =onOptionsClicked) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
        }
    }
}

/**
 * Previsualización de la pantalla
 */
@Preview(showBackground = true)
@Composable
fun AthletesScreenPreview() {
    HalterofiliaCaucaTheme {
        AthletesContent(
            athletes = listOf(
                Athlete(1, "Deportista 1"),
                Athlete(2, "Deportista 2"),
                Athlete(3, "Deportista 3")
            ),
            onAddAthleteClicked = {},
            onConnectBluetoothClicked = {},
            onLogoutClicked = {},
            onAthleteOptionsClicked = {}
        )
    }
}