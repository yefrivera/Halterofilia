package edu.unicauca.halterfilia_cauca.ui.features.measurement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.unicauca.halterfilia_cauca.data.FileRepository
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository
import edu.unicauca.halterfilia_cauca.domain.model.Measurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject

import edu.unicauca.halterfilia_cauca.core.bluetooth.BLEData
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController

enum class SlaveStatus {
    UNKNOWN,
    CHECKING,
    CONNECTED,
    ERROR
}

data class MeasurementState(
    val isConnected: Boolean = false,
    val calculatedValue: Float? = null,
    val isMeasuring: Boolean = false,
    val isReceiving: Boolean = false, // Nuevo estado
    val lastReceivedData: String = "",
    val lastMeasurementJson: String? = null
)

class MedidasViewModel(
    private val bluetoothController: BluetoothController,
    private val measurementRepository: MeasurementRepository,
    private val fileRepository: FileRepository, // Add FileRepository
    private val athleteId: String
) : ViewModel() {

    private val _state = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = _state.asStateFlow()

    init {
        // Observe connection state
        bluetoothController.connectedDevices
            .onEach { connectedMap ->
                val isConnected = connectedMap.isNotEmpty()
                _state.value = _state.value.copy(isConnected = isConnected)
            }
            .launchIn(viewModelScope)

        // Observe incoming data
        bluetoothController.measurementDataFlow
            .onEach { data ->
                data?.let {
                    Log.d("MedidasViewModel", "Datos recibidos del controller: $it") // LOG AÑADIDO
                    _state.value = _state.value.copy(
                        lastReceivedData = it,
                        isMeasuring = false, // Detener medición
                        isReceiving = false  // Finaliza la recepción
                    )
                    processIncomingData(it)
                    saveMeasurementToFile(it)
                    bluetoothController.clearMeasurementData() // Reset the flow
                }
            }
            .launchIn(viewModelScope)
    }

    

    fun startMeasurement() {
        if (_state.value.isConnected) {
            val connectedDeviceAddress = bluetoothController.connectedDevices.value.keys.firstOrNull()
            if (connectedDeviceAddress != null) {
                _state.value = _state.value.copy(isMeasuring = true, calculatedValue = null, lastReceivedData = "")
                bluetoothController.sendData(connectedDeviceAddress, "START")
            }
        }
    }

    fun stopMeasurement() {
        if (_state.value.isConnected) {
            val connectedDeviceAddress = bluetoothController.connectedDevices.value.keys.firstOrNull()
            if (connectedDeviceAddress != null) {
                _state.value = _state.value.copy(isReceiving = true) // Inicia la recepción
                bluetoothController.sendData(connectedDeviceAddress, "STOP")
            }
        }
    }

    private fun processIncomingData(data: String) {
        var masterAngle: Float? = null
        var slaveAngle: Float? = null

        val jsonObjects = data.trim().split('\n')

        for (jsonString in jsonObjects) {
            try {
                val json = JSONObject(jsonString)
                val id = json.getString("id")
                val angle = json.getDouble("angle").toFloat()

                if (id == "MASTER") {
                    masterAngle = angle
                } else if (id == "SLAVE") {
                    slaveAngle = angle
                }
            } catch (e: Exception) {
                // Ignore malformed JSON
            }
        }

        if (masterAngle != null && slaveAngle != null) {
            val result = masterAngle - slaveAngle
            _state.value = _state.value.copy(
                calculatedValue = result,
                isMeasuring = false
            )
        } else {
            _state.value = _state.value.copy(isMeasuring = false)
        }
    }


    fun saveMeasurement() {
        val currentValue = _state.value.calculatedValue
        if (currentValue != null) {
            viewModelScope.launch {
                val measurement = Measurement(
                    value = currentValue.toDouble(),
                    timestamp = System.currentTimeMillis(),
                    athleteId = athleteId
                )
                measurementRepository.saveMeasurement(measurement)
                _state.value = _state.value.copy(calculatedValue = null)
            }
        }
    }

    private fun saveMeasurementToFile(jsonData: String) {
        viewModelScope.launch {
            fileRepository.saveMeasurementToFile(jsonData)
        }
    }

    fun showLastMeasurement() {
        viewModelScope.launch {
            val result = fileRepository.readLastMeasurementFile()
            result.onSuccess {
                _state.value = _state.value.copy(lastMeasurementJson = it)
            }.onFailure {
                // Handle error, e.g., show a toast
            }
        }
    }

    fun clearLastMeasurementJson() {
        _state.value = _state.value.copy(lastMeasurementJson = null)
    }
}
