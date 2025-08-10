package edu.unicauca.halterfilia_cauca.data

import com.google.firebase.firestore.FirebaseFirestore
import edu.unicauca.halterfilia_cauca.domain.model.Measurement
import kotlinx.coroutines.tasks.await

class MeasurementRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveMeasurement(measurement: Measurement) {
        try {
            db.collection("athletes").document(measurement.athleteId)
                .collection("measurements")
                .add(measurement)
                .await()
        } catch (e: Exception) {
            // Handle exception
        }
    }
}
