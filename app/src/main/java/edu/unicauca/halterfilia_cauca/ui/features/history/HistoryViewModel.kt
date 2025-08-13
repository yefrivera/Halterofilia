package edu.unicauca.halterfilia_cauca.ui.features.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import edu.unicauca.halterfilia_cauca.data.MeasurementRepository
import edu.unicauca.halterfilia_cauca.domain.model.MeasurementSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class HistoryState(
    val sessions: List<MeasurementSession> = emptyList(),
    val filteredSessions: List<MeasurementSession> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val filterDate: Date? = null
)

class HistoryViewModel(
    private val repository: MeasurementRepository,
    private val athleteId: String
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadSessions()
    }

    private fun loadSessions() {
        if (userId == null) {
            _state.value = HistoryState(isLoading = false, error = "Usuario no autenticado.")
            return
        }

        viewModelScope.launch {
            _state.value = HistoryState(isLoading = true)
            val result = repository.getMeasurementSessions(userId, athleteId)
            if (result.isSuccess) {
                val sessions = result.getOrNull()?.sortedBy { it.timestamp } ?: emptyList()
                _state.value = HistoryState(isLoading = false, sessions = sessions, filteredSessions = sessions)
            } else {
                _state.value = HistoryState(isLoading = false, error = "Error al cargar las mediciones.")
            }
        }
    }

    fun filterSessionsByDate(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val filtered = _state.value.sessions.filter { session ->
            session.timestamp?.let {
                val sessionCalendar = Calendar.getInstance()
                sessionCalendar.time = it
                val sessionYear = sessionCalendar.get(Calendar.YEAR)
                val sessionMonth = sessionCalendar.get(Calendar.MONTH)
                val sessionDay = sessionCalendar.get(Calendar.DAY_OF_MONTH)
                sessionYear == year && sessionMonth == month && sessionDay == day
            } ?: false
        }
        _state.value = _state.value.copy(filteredSessions = filtered, filterDate = date)
    }

    fun clearFilter() {
        _state.value = _state.value.copy(filteredSessions = _state.value.sessions, filterDate = null)
    }

    fun deleteSession(measurementId: String) {
        if (userId == null) {
            Log.e("HistoryViewModel", "No se puede eliminar, el usuario no está logueado.")
            return
        }

        viewModelScope.launch {
            val result = repository.deleteMeasurementSession(userId, athleteId, measurementId)
            if (result.isSuccess) {
                // Recarga la lista de sesiones después de eliminar
                loadSessions()
            } else {
                // Opcional: manejar el estado de error en la UI
                Log.e("HistoryViewModel", "Error al eliminar la sesión")
            }
        }
    }
}