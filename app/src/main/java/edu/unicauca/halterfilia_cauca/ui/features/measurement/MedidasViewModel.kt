package edu.unicauca.halterfilia_cauca.ui.features.measurement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import edu.unicauca.halterfilia_cauca.core.bluetooth.BLEData
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository
import edu.unicauca.halterfilia_cauca.domain.model.DataPoint
import edu.unicauca.halterfilia_cauca.domain.model.MeasurementSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

// Clase de datos interna que representa la estructura del JSON recibido.
// Esto evita conflictos con la clase DataPoint oficial del proyecto.
data class IncomingDataPoint(
    val id: String = "",
    val accX: Double = 0.0,
    val accY: Double = 0.0,
    val accZ: Double = 0.0,
    val gyroX: Double = 0.0,
    val gyroY: Double = 0.0,
    val gyroZ: Double = 0.0,
    val angle: Double = 0.0
)

enum class SaveStatus {
    IDLE,
    SAVING,
    SUCCESS,
    ERROR
}

data class MeasurementState(
    val isConnected: Boolean = false,
    val isMeasuring: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.IDLE
)

class MedidasViewModel(
    private val bluetoothController: BluetoothController,
    private val measurementRepository: MeasurementRepository,
    private val athleteId: String
) : ViewModel() {

    private val _state = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = _state.asStateFlow()

    // La lista ahora almacena los objetos de datos que coinciden con el JSON.
    private val incomingDataPoints = mutableListOf<IncomingDataPoint>()

    init {
        bluetoothController.connectedDevices
            .onEach { connectedMap ->
                val isConnected = connectedMap.isNotEmpty()
                _state.value = _state.value.copy(isConnected = isConnected)
            }
            .launchIn(viewModelScope)

        bluetoothController.bleData
            .onEach { data ->
                if (data is BLEData.MeasurementData) {
                    processIncomingData(data.payload)
                }
            }
            .launchIn(viewModelScope)
    }

    fun startMeasurement() {
        if (_state.value.isConnected) {
            val connectedDeviceAddress = bluetoothController.connectedDevices.value.keys.firstOrNull()
            if (connectedDeviceAddress != null) {
                incomingDataPoints.clear()
                _state.value = _state.value.copy(isMeasuring = true, saveStatus = SaveStatus.IDLE)
                bluetoothController.sendData(connectedDeviceAddress, "START")
            }
        }
    }

    fun stopMeasurement() {
        if (_state.value.isConnected) {
            val connectedDeviceAddress = bluetoothController.connectedDevices.value.keys.firstOrNull()
            if (connectedDeviceAddress != null) {
                _state.value = _state.value.copy(isMeasuring = false)
                bluetoothController.sendData(connectedDeviceAddress, "STOP")
            }
        }
    }

    private fun processIncomingData(data: String) {
        try {
            // Transforma la cadena de objetos JSON separados por saltos de línea en un array JSON válido.
            val jsonArrayString = data.trim().split("\n").filter { it.isNotBlank() }.joinToString(separator = ",", prefix = "[", postfix = "]")

            val jsonArray = JSONArray(jsonArrayString)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val dataPoint = IncomingDataPoint(
                    id = json.optString("id", ""),
                    accX = json.optDouble("accX", 0.0),
                    accY = json.optDouble("accY", 0.0),
                    accZ = json.optDouble("accZ", 0.0),
                    gyroX = json.optDouble("gyroX", 0.0),
                    gyroY = json.optDouble("gyroY", 0.0),
                    gyroZ = json.optDouble("gyroZ", 0.0),
                    angle = json.optDouble("angle", 0.0)
                )
                incomingDataPoints.add(dataPoint)
            }
            Log.d("MedidasViewModel", "Datos procesados. Total de puntos: ${incomingDataPoints.size}")
            saveSession()

        } catch (e: JSONException) {
            Log.e("MedidasViewModel", "Error al parsear JSON: ${e.message}")
            _state.value = _state.value.copy(saveStatus = SaveStatus.ERROR)
        }
    }

    private fun saveSession() {
        if (incomingDataPoints.isEmpty()) {
            Log.w("MedidasViewModel", "No hay puntos de datos para guardar.")
            return
        }

        _state.value = _state.value.copy(saveStatus = SaveStatus.SAVING)

        // Mapea la lista de datos recibidos a la lista del modelo oficial DataPoint
        val officialDataPoints = incomingDataPoints.mapIndexed { index, incoming ->
            DataPoint(
                id = if (incoming.id.equals("MASTER", ignoreCase = true)) 0 else 1,
                idx = index,
                timeMs = 0L, // El JSON no provee timestamp por punto, se podría añadir si fuera necesario
                angle = incoming.angle.toFloat(),
                source = incoming.id
            )
        }

        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val userEmail = user?.email ?: "unknown_user"
            val userId = user?.uid ?: "unknown_user_id" // ID de usuario de Firebase Auth

            val session = MeasurementSession(
                userId = userId,
                athleteId = athleteId,
                userEmail = userEmail,
                dataPoints = officialDataPoints
            )

            Log.d("MedidasViewModel", "Iniciando el guardado de la sesión.")
            val result = measurementRepository.saveMeasurementSession(session)

            if (result.isSuccess) {
                _state.value = _state.value.copy(saveStatus = SaveStatus.SUCCESS)
                Log.i("MedidasViewModel", "Sesión guardada correctamente.")
            } else {
                _state.value = _state.value.copy(saveStatus = SaveStatus.ERROR)
                Log.e("MedidasViewModel", "Error al guardar la sesión", result.exceptionOrNull())
            }
        }
    }

    fun resetSaveStatus() {
        _state.value = _state.value.copy(saveStatus = SaveStatus.IDLE)
    }
}
