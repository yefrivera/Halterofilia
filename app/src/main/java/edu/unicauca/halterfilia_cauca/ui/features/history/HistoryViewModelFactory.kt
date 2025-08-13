package edu.unicauca.halterfilia_cauca.ui.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository

class HistoryViewModelFactory(
    private val measurementRepository: MeasurementRepository,
    private val athleteId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(measurementRepository, athleteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
