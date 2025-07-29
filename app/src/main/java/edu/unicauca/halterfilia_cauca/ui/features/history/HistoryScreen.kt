package edu.unicauca.halterfilia_cauca.ui.features.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

// Stateful
@Composable
fun HistoryScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = viewModel()
) {
    HistoryContent(
        selectedDate = historyViewModel.selectedDate,
        repetitions = historyViewModel.repetitionsForDate,
        isLoading = historyViewModel.isLoading,
        showCalendar = historyViewModel.showCalendar,
        onShowCalendar = { historyViewModel.onShowCalendar() },
        onDismissCalendar = { historyViewModel.onDismissCalendar() },
        onDateSelected = { historyViewModel.onDateSelected(it) },
        onRepetitionClicked = {
            // Ejemplo: Navegar a la pantalla de medidas
            navController.navigate(AppScreens.MedidasScreen.route)
        },
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}

// Stateless
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    selectedDate: LocalDate,
    repetitions: List<RepetitionHistoryItem>,
    isLoading: Boolean,
    showCalendar: Boolean,
    onShowCalendar: () -> Unit,
    onDismissCalendar: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    onRepetitionClicked: (RepetitionHistoryItem) -> Unit, // Recibir la nueva acción
    onNavigateBack: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = onDismissCalendar,
            confirmButton = {
                TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissCalendar) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Texto para la opción de registro
            Text(text = "Buscar por fecha")

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onShowCalendar, modifier = Modifier.fillMaxWidth()) {
                Text(text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, "Seleccionar fecha")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> CircularProgressIndicator()
                repetitions.isEmpty() -> Text("No hay datos disponibles en esta fecha.")
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(repetitions) { item ->
                            // ✅ Se llama a la nueva versión del HistoryItem
                            HistoryItem(
                                item = item,
                                onClick = { onRepetitionClicked(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ HistoryItem MODIFICADO
 * Ocupa todo el ancho y es clicable
 */
@Composable
fun HistoryItem(item: RepetitionHistoryItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(50), // Forma de píldora
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8BC34A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Se hace clicable
    ) {
        Text(
            text = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center // El texto se centra para mejor apariencia
        )
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HalterofiliaCaucaTheme {
        HistoryContent(
            selectedDate = LocalDate.now(),
            repetitions = listOf(RepetitionHistoryItem(1, "Repetición 1"), RepetitionHistoryItem(2, "Repetición 2")),
            isLoading = false,
            showCalendar = false,
            onShowCalendar = {},
            onDismissCalendar = {},
            onDateSelected = {},
            onRepetitionClicked = {}, // Se añade la acción vacía para el preview
            onNavigateBack = {}
        )
    }
}