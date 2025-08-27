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
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONException
import java.util.Date
// Clase de datos interna que representa la estructura del JSON recibido.
// Esto evita conflictos con la clase DataPoint oficial del proyecto.
data class IncomingDataPoint(
    val id: String = "",
    val idx: Int = 0,
    val angle: Double = 0.0,
    val time: Long = 0L
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
    val isStopping: Boolean = false,
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
                if (data is BLEData.MeasurementData && _state.value.isMeasuring) {
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
                _state.value = _state.value.copy(
                    isMeasuring = true,
                    saveStatus = SaveStatus.IDLE,
                    isStopping = false
                )
                bluetoothController.sendData(connectedDeviceAddress, "START")
            }
        }
    }

    fun stopMeasurement() {
        if (_state.value.isConnected && _state.value.isMeasuring && !_state.value.isStopping) {
            val connectedDeviceAddress = bluetoothController.connectedDevices.value.keys.firstOrNull()
            if (connectedDeviceAddress != null) {
                _state.value = _state.value.copy(isStopping = true)
                bluetoothController.sendData(connectedDeviceAddress, "STOP")

                // Launch a coroutine to save after a delay.
                // This allows any in-flight data to be processed before saving.
                viewModelScope.launch {
                    delay(1000) // 1-second delay
                    saveSession()
                }
            }
        }
    }

    private fun processIncomingData(data: String) {
        var searchIndex = 0
        var pointsAddedInBatch = 0

        while (searchIndex < data.length) {
            val startIndex = data.indexOf('{', searchIndex)
            if (startIndex == -1) {
                break
            }

            var braceCount = 0
            var endIndex = -1
            for (i in startIndex until data.length) {
                when (data[i]) {
                    '{' -> braceCount++
                    '}' -> braceCount--
                }
                if (braceCount == 0) {
                    endIndex = i
                    break
                }
            }

            if (endIndex != -1) {
                val jsonString = data.substring(startIndex, endIndex + 1)
                try {
                    val json = JSONObject(jsonString)
                    val dataPoint = IncomingDataPoint(
                        id = json.optString("id", ""),
                        idx = json.optInt("idx", 0),
                        angle = json.optDouble("angle", 0.0),
                        time = json.optLong("time", 0L)
                    )
                    incomingDataPoints.add(dataPoint)
                    pointsAddedInBatch++
                } catch (e: JSONException) {
                    Log.w("MedidasViewModel", "Ignoring block that is not valid JSON: $jsonString")
                }
                searchIndex = endIndex + 1
            } else {
                break
            }
        }
        if (pointsAddedInBatch > 0) {
            Log.d("MedidasViewModel", "Stateless parse: Added $pointsAddedInBatch points. Total: ${incomingDataPoints.size}")
        }
    }

    private fun saveSession() {
        // Prevent saving if no data or if a save is already in progress.
        if (incomingDataPoints.isEmpty() || _state.value.saveStatus == SaveStatus.SAVING) {
            Log.w("MedidasViewModel", "Save attempt ignored: No data or save already in progress.")
            // If there's no data, we can consider the "stopping" sequence finished.
            if (incomingDataPoints.isEmpty()) {
                _state.value = _state.value.copy(isMeasuring = false, isStopping = false)
            }
            return
        }

        _state.value = _state.value.copy(saveStatus = SaveStatus.SAVING)

        // Mapea la lista de datos recibidos a la lista del modelo oficial DataPoint
        val officialDataPoints = incomingDataPoints.map { incoming ->
            DataPoint(
                id = if (incoming.id.equals("MASTER", ignoreCase = true)) 0 else 1,
                idx = incoming.idx,
                time = incoming.time,
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
                timestamp = Date(),
                dataPoints = officialDataPoints
            )

            Log.d("MedidasViewModel", "Saving final session with ${officialDataPoints.size} data points.")
            val result = measurementRepository.saveMeasurementSession(session)

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    saveStatus = SaveStatus.SUCCESS,
                    isMeasuring = false,
                    isStopping = false
                )
                Log.i("MedidasViewModel", "Session saved successfully.")
            } else {
                _state.value = _state.value.copy(
                    saveStatus = SaveStatus.ERROR,
                    isMeasuring = false,
                    isStopping = false
                )
                Log.e("MedidasViewModel", "Error saving session", result.exceptionOrNull())
            }
        }
    }


    fun resetSaveStatus() {
        _state.value = _state.value.copy(saveStatus = SaveStatus.IDLE)
    }
}
