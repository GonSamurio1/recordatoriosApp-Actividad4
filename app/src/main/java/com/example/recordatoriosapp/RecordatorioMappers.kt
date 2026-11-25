package com.example.recordatoriosapp

import com.google.firebase.firestore.DocumentSnapshot

// 1. De Entity (Room) -> Modelo de Dominio (UI)
fun RecordatorioEntity.toDomain(): Recordatorio {
    val estadoDominio = when (this.estado) {
        "Completado" -> EstadoRecordatorio.Completado
        "EnProceso" -> EstadoRecordatorio.EnProceso
        else -> EstadoRecordatorio.Pendiente
    }

    val prioridadDominio = try {
        Prioridad.valueOf(this.prioridad)
    } catch (_: IllegalArgumentException) {
        Prioridad.MEDIA
    }

    val syncDominio = try {
        EstadoSync.valueOf(this.estadoSync)
    } catch (_: IllegalArgumentException) {
        EstadoSync.SINCRONIZADO
    }

    return Recordatorio(
        id = this.id,
        titulo = this.titulo,
        descripcion = this.descripcion,
        categoria = this.categoria,
        prioridad = prioridadDominio,
        estado = estadoDominio,
        fechaCreacion = this.fechaCreacion,
        estadoSync = syncDominio
    )
}

// 2. De Modelo de Dominio (UI) -> Entity (Room)
fun Recordatorio.toEntity(
    userId: String,
    estadoSync: EstadoSync = this.estadoSync
): RecordatorioEntity {
    val estadoTexto = when (this.estado) {
        is EstadoRecordatorio.Completado -> "Completado"
        is EstadoRecordatorio.EnProceso -> "EnProceso"
        is EstadoRecordatorio.Pendiente -> "Pendiente"
    }

    return RecordatorioEntity(
        id = this.id,
        userId = userId,
        titulo = this.titulo,
        descripcion = this.descripcion,
        categoria = this.categoria,
        prioridad = this.prioridad.name,
        estado = estadoTexto,
        fechaCreacion = this.fechaCreacion,
        estadoSync = estadoSync.name
    )
}

// 3. De Entity (Room) -> Mapa para Cloud Firestore
fun RecordatorioEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to this.id,
        "userId" to this.userId,
        "titulo" to this.titulo,
        "descripcion" to this.descripcion,
        "categoria" to this.categoria,
        "prioridad" to this.prioridad,
        "estado" to this.estado,
        "fechaCreacion" to this.fechaCreacion
    )
}

// 4. De DocumentSnapshot (Cloud Firestore) -> Entity (Room)
fun DocumentSnapshot.toRecordatorioEntity(userId: String): RecordatorioEntity? {
    val titulo = getString("titulo") ?: return null
    return RecordatorioEntity(
        id = id,
        userId = userId,
        titulo = titulo,
        descripcion = getString("descripcion"),
        categoria = getString("categoria") ?: "General",
        prioridad = getString("prioridad") ?: Prioridad.MEDIA.name,
        estado = getString("estado") ?: "Pendiente",
        fechaCreacion = getLong("fechaCreacion") ?: System.currentTimeMillis(),
        estadoSync = EstadoSync.SINCRONIZADO.name
    )
}