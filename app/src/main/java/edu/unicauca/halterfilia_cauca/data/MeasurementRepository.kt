package edu.unicauca.halterfilia_cauca.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import edu.unicauca.halterfilia_cauca.domain.model.MeasurementSession
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.Query

class MeasurementRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getMeasurementSessions(userId: String, athleteId: String): Result<List<MeasurementSession>> {
        return try {
            val querySnapshot = db.collection("users").document(userId)
                .collection("athletes").document(athleteId)
                .collection("measurements")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena por fecha, más nuevas primero
                .get()
                .await()

            val sessions = querySnapshot.toObjects(MeasurementSession::class.java)
            Log.d("MeasurementRepository", "Sesiones obtenidas: ${sessions.size}")
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e("MeasurementRepository", "Error al obtener las sesiones", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMeasurementSession(userId: String, athleteId: String, measurementId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .collection("athletes").document(athleteId)
                .collection("measurements").document(measurementId)
                .delete()
                .await()
            Log.d("MeasurementRepository", "Sesión eliminada: $measurementId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MeasurementRepository", "Error al eliminar la sesión", e)
            Result.failure(e)
        }
    }

    suspend fun saveMeasurementSession(session: MeasurementSession): Result<Unit> {
        return try {
            Log.d("MeasurementRepository", "Intentando guardar sesión para el atleta: ${session.athleteId} del usuario ${session.userId}")
            db.collection("users").document(session.userId)
                .collection("athletes").document(session.athleteId)
                .collection("measurements")
                .add(session) // Guarda el objeto de sesión completo
                .await()
            Log.i("MeasurementRepository", "Sesión guardada exitosamente en Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MeasurementRepository", "Error al guardar en Firestore", e)
            Result.failure(e)
        }
    }
}
