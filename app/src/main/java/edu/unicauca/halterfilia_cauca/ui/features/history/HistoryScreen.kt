package edu.unicauca.halterfilia_cauca.ui.features.history

import android.app.DatePickerDialog
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.MotionEvent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository
import edu.unicauca.halterfilia_cauca.domain.model.MeasurementSession
import java.text.SimpleDateFormat
import java.util.*

// Sealed class para gestionar qué diálogo se muestra. Es una práctica más limpia.
private sealed class DialogState {
    object Hidden : DialogState()
    data class Deleting(val session: MeasurementSession) : DialogState()
    data class ViewingChart(val session: MeasurementSession) : DialogState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    athleteName: String,
    athleteDocId: String
) {
    // El ViewModel ya obtiene el repositorio a través del Factory,
    // no es necesario instanciarlo aquí.
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(MeasurementRepository(), athleteDocId)
    )
    val state by viewModel.state.collectAsState()

    var dialogState by remember { mutableStateOf<DialogState>(DialogState.Hidden) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            viewModel.filterSessionsByDate(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de $athleteName",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Buscar por fecha")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text(text = state.error!!)
                state.sessions.isEmpty() -> Text(text = "No hay mediciones registradas.")
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Barra que muestra el estado del filtro
                        state.filterDate?.let { date ->
                            FilterStatusBar(
                                filterDate = date,
                                onClearFilter = { viewModel.clearFilter() }
                            )
                        }

                        // Lista de sesiones
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(state.filteredSessions) { index, session ->
                                RepetitionItem(
                                    repetitionNumber = index + 1,
                                    session = session,
                                    onDeleteClicked = { dialogState = DialogState.Deleting(session) },
                                    onItemClicked = { dialogState = DialogState.ViewingChart(session) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manejo centralizado de diálogos
    when (val currentDialog = dialogState) {
        is DialogState.Hidden -> { /* No hacer nada */ }
        is DialogState.Deleting -> {
            DeleteConfirmationDialog(
                onConfirm = {
                    currentDialog.session.id?.let { viewModel.deleteSession(it) }
                    dialogState = DialogState.Hidden
                },
                onDismiss = { dialogState = DialogState.Hidden }
            )
        }
        is DialogState.ViewingChart -> {
            ChartModal(
                session = currentDialog.session,
                onDismissRequest = { dialogState = DialogState.Hidden }
            )
        }
    }
}

@Composable
private fun FilterStatusBar(filterDate: Date, onClearFilter: () -> Unit) {
    val formattedDate = remember(filterDate) {
        SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(filterDate)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Filtro: $formattedDate",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClearFilter) {
            Text("Limpiar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepetitionItem(
    repetitionNumber: Int,
    session: MeasurementSession,
    onDeleteClicked: () -> Unit,
    onItemClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    Card(
        onClick = onItemClicked,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Repetición $repetitionNumber",
                    style = MaterialTheme.typography.titleMedium
                )
                session.timestamp?.let {
                    Text(
                        text = dateFormatter.format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            onDeleteClicked()
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Eliminación") },
        text = { Text("¿Estás seguro de que quieres eliminar esta sesión? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartModal(session: MeasurementSession, onDismissRequest: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isLandscape) 32.dp else 16.dp,
                    vertical = if (isLandscape) 16.dp else 32.dp
                )
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Gráfico de Medición", style = MaterialTheme.typography.titleLarge) }
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismissRequest) {
                            Text("Cerrar")
                        }
                    }
                }
            ) { paddingValues ->
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)

                            // Listener para controlar la visibilidad de los valores al hacer zoom
                            onChartGestureListener = object : OnChartGestureListener {
                                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                                override fun onChartLongPressed(me: MotionEvent?) {}
                                override fun onChartDoubleTapped(me: MotionEvent?) {}
                                override fun onChartSingleTapped(me: MotionEvent?) {}
                                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}

                                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                                    this@apply.data?.let { lineData ->
                                        val dataSet = lineData.getDataSetByIndex(0) as LineDataSet
                                        // Muestra los valores solo si el zoom en X es mayor a 1.5f
                                        dataSet.setDrawValues(this@apply.viewPortHandler.scaleX > 1.5f)
                                        invalidate()
                                    }
                                }
                            }
                        }
                    },
                    update = { chart ->
                        // 1. Separar datos por fuente (MASTER y SLAVE)
                        val dataBySource = session.dataPoints.groupBy { it.source }
                        val masterData = dataBySource["MASTER"]?.associateBy { it.idx } ?: emptyMap()
                        val slaveData = dataBySource["SLAVE"]?.associateBy { it.idx } ?: emptyMap()

                        // 2. Determinar el rango de idx
                        val maxMasterIdx = masterData.keys.maxOrNull() ?: -1
                        val maxSlaveIdx = slaveData.keys.maxOrNull() ?: -1
                        val maxIdx = maxOf(maxMasterIdx, maxSlaveIdx)

                        // 3. Calcular la diferencia absoluta y crear las entradas para el gráfico
                        val entries = mutableListOf<Entry>()
                        if (maxIdx != -1) {
                            for (i in 0..maxIdx) {
                                val masterPoint = masterData[i]
                                val slavePoint = slaveData[i]

                                val masterAngle = masterPoint?.angle ?: 0f
                                val slaveAngle = slavePoint?.angle ?: 0f

                                val calculatedAngle = kotlin.math.abs(masterAngle - slaveAngle)

                                // Usar el tiempo del MASTER como referencia, o del SLAVE si no existe
                                val time = (masterPoint?.time ?: slavePoint?.time ?: 0L).toFloat()

                                entries.add(Entry(time, calculatedAngle))
                            }
                        }

                        val dataSet = LineDataSet(entries, "Ángulo (°) vs Tiempo (ms)")

                        // Ocultar los valores inicialmente, comprobando el nivel de zoom actual
                        dataSet.setDrawValues(chart.viewPortHandler.scaleX > 1.5f)

                        val lineData = LineData(dataSet)
                        chart.data = lineData
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}
