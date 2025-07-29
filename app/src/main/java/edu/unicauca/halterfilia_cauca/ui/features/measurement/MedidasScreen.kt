package edu.unicauca.halterfilia_cauca.ui.features.measurement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme

import androidx.navigation.NavController

/**
 * Composable Stateful
 */
@Composable
fun MedidasScreen(
    navController: NavController,
    measureViewModel: MeasureViewModel = viewModel()
) {
    MeasureContent(
        athleteName = measureViewModel.athleteName,
        isMeasuring = measureViewModel.isMeasuring,
        repetitions = measureViewModel.repetitions,
        onStartClicked = { measureViewModel.onStartMeasuring() },
        onStopClicked = { measureViewModel.onStopMeasuring() },
        onRepetitionOptionsClicked = {
            // Ejemplo: Volver a la pantalla anterior
            navController.popBackStack()
        },
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}

/**
 * Composable Stateless que dibuja la UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureContent(
    athleteName: String,
    isMeasuring: Boolean,
    repetitions: List<Repetition>,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onRepetitionOptionsClicked: (Repetition) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(athleteName) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tomar medidas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Botones de Iniciar y Parar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onStartClicked, enabled = !isMeasuring) {
                    Text("Iniciar")
                }
                Button(onClick = onStopClicked, enabled = isMeasuring) {
                    Text("Parar")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de repeticiones
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre items
            ) {
                items(repetitions) { repetition ->
                    RepetitionItem(
                        repetition = repetition,
                        onOptionsClicked = { onRepetitionOptionsClicked(repetition) }
                    )
                }
            }
        }
    }
}

/**
 * Composable para un solo item de la lista de repeticiones
 */
@Composable
fun RepetitionItem(
    repetition: Repetition,
    onOptionsClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip de Repetición
        Card(
            shape = RoundedCornerShape(50), // Forma de píldora
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8BC34A)) // Verde
        ) {
            Text(
                text = repetition.name,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Chip de Ángulo
        Card(
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = repetition.angle?.let { "$it°" } ?: "Ángulo",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Botón de opciones
        IconButton(onClick = onOptionsClicked) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
        }
    }
}

/**
 * Previsualización
 */
@Preview(showBackground = true)
@Composable
fun MeasureScreenPreview() {
    HalterofiliaCaucaTheme {
        MeasureContent(
            athleteName = "Deportista X",
            isMeasuring = false,
            repetitions = listOf(
                Repetition(1, "Repetición 1", 89.5f),
                Repetition(2, "Repetición 2", 91.2f),
                Repetition(3, "Repetición 3")
            ),
            onStartClicked = {},
            onStopClicked = {},
            onRepetitionOptionsClicked = {},
            onNavigateBack = {}
        )
    }
}