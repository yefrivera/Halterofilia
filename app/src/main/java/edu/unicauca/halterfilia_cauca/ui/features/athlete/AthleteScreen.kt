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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme


import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

// 1. Define las acciones posibles en el menú para un manejo más limpio
enum class AthleteMenuAction {
    CONSULT,
    MEASURE,
    HISTORY,
    DELETE
}

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
            navController.navigate(AppScreens.AthleteRegistrationScreen.route)
        },
        onConnectBluetoothClicked = {
            navController.navigate(AppScreens.BluetoothScreen.route)
        },
        onLogoutClicked = {
            navController.navigate(AppScreens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        },
        // 2. Maneja la acción seleccionada del menú
        onAthleteOptionSelected = { athlete, action ->
            // Aquí defines qué hacer para cada opción del menú
            // Puedes pasar el ID del deportista como argumento en la ruta
            when (action) {
                AthleteMenuAction.CONSULT -> { /* TODO: navController.navigate("consult_screen/${athlete.id}") */ }
                AthleteMenuAction.MEASURE -> navController.navigate(AppScreens.MedidasScreen.route)
                AthleteMenuAction.HISTORY -> navController.navigate(AppScreens.HistoryScreen.route)
                AthleteMenuAction.DELETE -> { /* TODO: Mostrar diálogo de confirmación y luego llamar al ViewModel */ }
            }
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
    // 3. Se actualiza la firma para pasar la acción seleccionada
    onAthleteOptionSelected: (Athlete, AthleteMenuAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera con título y botón de salir
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

        // Lista de deportistas (scrollable)
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(athletes) { athlete ->
                AthleteItem(
                    athlete = athlete,
                    onOptionSelected = { action ->
                        onAthleteOptionSelected(athlete, action)
                    }
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(44.dp))

        // Botones inferiores
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp)
        ) {
            Button(onClick = onAddAthleteClicked, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Agregar deportista", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onConnectBluetoothClicked, modifier = Modifier.fillMaxWidth()) {
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
    // 4. Se cambia el callback para que informe qué acción se seleccionó
    onOptionSelected: (AthleteMenuAction) -> Unit
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
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = athlete.name,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp
        )

        // 5. Lógica del menú desplegable
        var expanded by remember { mutableStateOf(false) }

        Box {
            // Este botón abre el menú
            IconButton(onClick = { expanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
            }

            // Aquí se define el menú
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Consultar datos") },
                    onClick = {
                        onOptionSelected(AthleteMenuAction.CONSULT)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tomar medidas") },
                    onClick = {
                        onOptionSelected(AthleteMenuAction.MEASURE)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Ver historial") },
                    onClick = {
                        onOptionSelected(AthleteMenuAction.HISTORY)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar deportista") },
                    onClick = {
                        onOptionSelected(AthleteMenuAction.DELETE)
                        expanded = false
                    }
                )
            }
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
            // Se actualiza la preview para que no de error
            onAthleteOptionSelected = { _, _ -> }
        )
    }
}