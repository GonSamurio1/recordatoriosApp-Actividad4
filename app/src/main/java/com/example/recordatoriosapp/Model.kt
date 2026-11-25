 package com.example.recordatoriosapp
 import java.util.UUID

    sealed class EstadoRecordatorio(val descripcion: String) {
        object Pendiente : EstadoRecordatorio("Pendiente")
        object EnProceso : EstadoRecordatorio("En Proceso")
        object Completado : EstadoRecordatorio("Completado")
    }

    enum class Prioridad {
        BAJA, MEDIA, ALTA
    }

    data class Recordatorio(
        val id: String = UUID.randomUUID().toString(),
        val titulo: String,
        val descripcion: String? = null,
        val categoria: String,
        val prioridad: Prioridad,
        val estado: EstadoRecordatorio,
        val fechaCreacion: Long = System.currentTimeMillis(),
        val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
    )
