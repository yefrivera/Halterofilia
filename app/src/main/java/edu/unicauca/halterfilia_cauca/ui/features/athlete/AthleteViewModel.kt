package edu.unicauca.halterfilia_cauca.ui.features.athlete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.unicauca.halterfilia_cauca.domain.model.Athlete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Define los estados para la UI
sealed interface AthleteUiState {
    object Loading : AthleteUiState
    data class Success(
        val athletes: List<Athlete>,
        val selectedAthlete: Athlete? = null,
        val showConsultDialog: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val showLogoutConfirmation: Boolean = false
    ) : AthleteUiState
    object Empty : AthleteUiState
    data class Error(val message: String) : AthleteUiState
}

class AthleteViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<AthleteUiState>(AthleteUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchAthletes()
    }

    private fun fetchAthletes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = AthleteUiState.Error("Usuario no autenticado.")
            return
        }

        firestore.collection("users").document(userId).collection("athletes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = AthleteUiState.Error(e.message ?: "Error al cargar los deportistas.")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val athletesList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Athlete::class.java)?.copy(id = doc.id)
                    }
                    if (athletesList.isEmpty()) {
                        _uiState.value = AthleteUiState.Empty
                    } else {
                        _uiState.value = AthleteUiState.Success(athletesList)
                    }
                } else {
                    _uiState.value = AthleteUiState.Empty
                }
            }
    }

    fun onConsultAthleteClicked(athlete: Athlete) {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(selectedAthlete = athlete, showConsultDialog = true) ?: it
        }
    }

    fun onUpdateAthlete(athlete: Athlete) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null || athlete.id == null) {
                // Handle error
                return@launch
            }
            try {
                firestore.collection("users").document(userId).collection("athletes")
                    .document(athlete.id)
                    .set(athlete)
                    .await()
                dismissConsultDialog()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onDeleteAthleteClicked(athlete: Athlete) {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(selectedAthlete = athlete, showDeleteConfirmation = true) ?: it
        }
    }

    fun confirmAthleteDeletion() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            val athleteToDelete = (_uiState.value as? AthleteUiState.Success)?.selectedAthlete

            if (userId == null || athleteToDelete?.id == null) {
                // Handle error
                return@launch
            }

            try {
                firestore.collection("users").document(userId).collection("athletes")
                    .document(athleteToDelete.id)
                    .delete()
                    .await()
                dismissDeleteConfirmation()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onLogoutClicked() {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(showLogoutConfirmation = true)
                ?: (it as? AthleteUiState.Empty)?.let { AthleteUiState.Success(emptyList(), showLogoutConfirmation = true) }
                ?: it
        }
    }

    fun confirmLogout(onLoggedOut: () -> Unit) {
        auth.signOut()
        onLoggedOut()
        dismissLogoutConfirmation()
    }

    fun dismissConsultDialog() {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(selectedAthlete = null, showConsultDialog = false) ?: it
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(selectedAthlete = null, showDeleteConfirmation = false) ?: it
        }
    }

    fun dismissLogoutConfirmation() {
        _uiState.update {
            (it as? AthleteUiState.Success)?.copy(showLogoutConfirmation = false) ?: it
        }
    }
}
