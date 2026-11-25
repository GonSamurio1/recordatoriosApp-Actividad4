package com.example.recordatoriosapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun coleccionUsuario(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("reminders")

    // Crea o actualiza usando el UUID estable como ID del documento (idempotente, sin duplicados)
    suspend fun guardarOActualizar(userId: String, entity: RecordatorioEntity) {
        coleccionUsuario(userId)
            .document(entity.id)
            .set(entity.toFirestoreMap(), SetOptions.merge())
            .await()
    }

    // Elimina el documento remoto por su UUID estable
    suspend fun eliminar(userId: String, reminderId: String) {
        coleccionUsuario(userId)
            .document(reminderId)
            .delete()
            .await()
    }

    // Descarga todos los recordatorios del usuario autenticado
    suspend fun obtenerTodos(userId: String): List<RecordatorioEntity> {
        val snapshot = coleccionUsuario(userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toRecordatorioEntity(userId)
        }
    }

    // Prueba explícita de seguridad (Consigna 3): intenta leer registros de otro userId
    suspend fun probarAccesoOtroUsuario(otroUserId: String): Result<Int> {
        return try {
            val snapshot = coleccionUsuario(otroUserId).get().await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}