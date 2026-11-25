package com.example.recordatoriosapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    // Listado reactivo filtrado estrictamente por el usuario autenticado
    @Query(
        "SELECT * FROM recordatorios " +
                "WHERE userId = :userId AND estadoSync != 'PENDIENTE_ELIMINACION' " +
                "ORDER BY fechaCreacion DESC"
    )
    fun observarTodos(userId: String): Flow<List<RecordatorioEntity>>

    // Consulta filtrada por usuario y estado
    @Query(
        "SELECT * FROM recordatorios " +
                "WHERE userId = :userId AND estado = :estado AND estadoSync != 'PENDIENTE_ELIMINACION' " +
                "ORDER BY fechaCreacion DESC"
    )
    fun observarPorEstado(userId: String, estado: String): Flow<List<RecordatorioEntity>>

    // Obtener un recordatorio específico por su UUID estable y usuario
    @Query("SELECT * FROM recordatorios WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun obtenerPorId(id: String, userId: String): RecordatorioEntity?

    // Obtener operaciones pendientes de sincronización de ese usuario
    @Query("SELECT * FROM recordatorios WHERE userId = :userId AND estadoSync != 'SINCRONIZADO'")
    suspend fun obtenerPendientesDeSync(userId: String): List<RecordatorioEntity>

    // Insertar o reemplazar (idempotente gracias al UUID estable)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(recordatorio: RecordatorioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(recordatorios: List<RecordatorioEntity>)

    @Update
    suspend fun actualizar(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminar(recordatorio: RecordatorioEntity)

    @Query("DELETE FROM recordatorios WHERE id = :id AND userId = :userId")
    suspend fun eliminarPorId(id: String, userId: String)

    // Limpiar registros locales de un usuario al cerrar sesión (opcional o controlado)
    @Query("DELETE FROM recordatorios WHERE userId = :userId")
    suspend fun eliminarTodosDeUsuario(userId: String)
}