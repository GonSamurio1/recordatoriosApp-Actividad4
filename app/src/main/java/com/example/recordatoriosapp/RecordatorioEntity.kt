package com.example.recordatoriosapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EstadoSync {
    SINCRONIZADO,
    PENDIENTE_CREACION,
    PENDIENTE_ACTUALIZACION,
    PENDIENTE_ELIMINACION,
    ERROR_SYNC
}

@Entity(tableName = "recordatorios")
data class RecordatorioEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val titulo: String,
    val descripcion: String?,
    val categoria: String,
    val prioridad: String,
    val estado: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val estadoSync: String = EstadoSync.PENDIENTE_CREACION.name
)