package edu.unicauca.halterfilia_cauca.ui.features.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.unicauca.halterfilia_cauca.core.bluetooth.BluetoothController
import edu.unicauca.halterfilia_cauca.data.FileRepository
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository

@Suppress("UNCHECKED_CAST")
class MedidasViewModelFactory(
    private val bluetoothController: BluetoothController,
    private val measurementRepository: MeasurementRepository,
    private val fileRepository: FileRepository, // Add FileRepository
    private val athleteId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedidasViewModel::class.java)) {
            return MedidasViewModel(bluetoothController, measurementRepository, fileRepository, athleteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
