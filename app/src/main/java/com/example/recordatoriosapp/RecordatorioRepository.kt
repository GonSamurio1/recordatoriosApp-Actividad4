package com.example.recordatoriosapp

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

sealed class ResultadoSync {
    object Exito : ResultadoSync()
    data class ErrorControlado(val mensaje: String) : ResultadoSync()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecordatorioRepository(
    private val dao: RecordatorioDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val authDataSource: FirebaseAuthDataSource
) {
    private val syncMutex = Mutex()

    // Room sigue siendo la fuente única de verdad, pero aislada dinámicamente por el uid activo
    val recordatoriosFlow: Flow<List<Recordatorio>> =
        authDataSource.observarEstadoAutenticacion()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    dao.observarTodos(user.uid).map { entidades ->
                        entidades.map { it.toDomain() }
                    }
                }
            }

    suspend fun obtenerPorId(id: String): Recordatorio? {
        val uid = authDataSource.uidActual ?: return null
        return dao.obtenerPorId(id, uid)?.toDomain()
    }

    // 1. CREAR (Local-First con UUID estable + userId)
    suspend fun crearRecordatorio(
        titulo: String,
        descripcion: String?,
        categoria: String,
        prioridad: Prioridad,
        estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente
    ): ResultadoSync {
        val uid = authDataSource.uidActual
            ?: return ResultadoSync.ErrorControlado("Debés iniciar sesión para guardar recordatorios.")

        val nuevo = Recordatorio(
            id = UUID.randomUUID().toString(),
            titulo = titulo,
            descripcion = descripcion,
            categoria = categoria,
            prioridad = prioridad,
            estado = estado,
            estadoSync = EstadoSync.PENDIENTE_CREACION
        )
        dao.insertar(nuevo.toEntity(userId = uid, estadoSync = EstadoSync.PENDIENTE_CREACION))

        return sincronizarPendientes()
    }

    // 2. ACTUALIZAR (Local-First manteniendo el mismo UUID estable)
    suspend fun actualizarRecordatorio(recordatorio: Recordatorio): ResultadoSync {
        val uid = authDataSource.uidActual
            ?: return ResultadoSync.ErrorControlado("Sesión no válida.")

        val existente = dao.obtenerPorId(recordatorio.id, uid)
            ?: return ResultadoSync.ErrorControlado("Recordatorio no encontrado.")

        val nuevoEstadoSync = if (existente.estadoSync == EstadoSync.PENDIENTE_CREACION.name) {
            EstadoSync.PENDIENTE_CREACION
        } else {
            EstadoSync.PENDIENTE_ACTUALIZACION
        }

        val entidadActualizada = recordatorio.toEntity(
            userId = uid,
            estadoSync = nuevoEstadoSync
        )
        dao.actualizar(entidadActualizada)

        return sincronizarPendientes()
    }

    // 3. ELIMINAR (Si nunca subió a Firestore se borra local; si ya existía, se marca pendiente)
    suspend fun eliminarRecordatorio(id: String): ResultadoSync {
        val uid = authDataSource.uidActual
            ?: return ResultadoSync.ErrorControlado("Sesión no válida.")

        val existente = dao.obtenerPorId(id, uid) ?: return ResultadoSync.Exito

        if (existente.estadoSync == EstadoSync.PENDIENTE_CREACION.name) {
            dao.eliminarPorId(id, uid)
            return ResultadoSync.Exito
        } else {
            dao.actualizar(existente.copy(estadoSync = EstadoSync.PENDIENTE_ELIMINACION.name))
            return sincronizarPendientes()
        }
    }

    // 4. SINCRONIZACIÓN CONTROLADA CON CLOUD FIRESTORE (Sube pendientes + descarga nube sin duplicar)
    suspend fun sincronizarConServidor(descargarRemotos: Boolean = true): ResultadoSync {
        val uid = authDataSource.uidActual
            ?: return ResultadoSync.ErrorControlado("No hay una sesión activa para sincronizar.")

        return syncMutex.withLock {
            try {
                val resultadoSubida = procesarColaPendientesInterna(uid)
                if (resultadoSubida is ResultadoSync.ErrorControlado) {
                    return@withLock resultadoSubida
                }

                if (descargarRemotos) {
                    val remotos = firestoreDataSource.obtenerTodos(uid)
                    val idsRemotos = remotos.map { it.id }.toSet()

                    for (remoto in remotos) {
                        val local = dao.obtenerPorId(remoto.id, uid)
                        // Solo sobreescribimos si no existe en local o si ya estaba sincronizado (respetamos ediciones offline)
                        if (local == null || local.estadoSync == EstadoSync.SINCRONIZADO.name) {
                            dao.insertar(remoto)
                        }
                    }

                    // Si se borró desde otro dispositivo, reflejarlo localmente si estaba sincronizado
                    val localesActuales = dao.obtenerPendientesDeSync(uid) // verificamos pendientes antes
                    if (localesActuales.isEmpty() && idsRemotos.isEmpty()) {
                        // Opcional: mantener consistencia si la nube quedó vacía
                    }
                }

                ResultadoSync.Exito
            } catch (e: Exception) {
                interpretarExcepcionFirebase(e)
            }
        }
    }

    // Prueba de verificación de reglas de seguridad (Consigna 3)
    suspend fun verificarAislamientoSeguridad(uidAjeno: String): String {
        val resultado = firestoreDataSource.probarAccesoOtroUsuario(uidAjeno)
        return resultado.fold(
            onSuccess = { "ALERTA: Se pudieron leer $it documentos de otro usuario." },
            onFailure = { error ->
                "Bloqueo exitoso por Reglas de Firestore: ${error.localizedMessage ?: "PERMISSION_DENIED"}"
            }
        )
    }

    private suspend fun sincronizarPendientes(): ResultadoSync {
        val uid = authDataSource.uidActual
            ?: return ResultadoSync.ErrorControlado("Sin sesión activa.")

        return syncMutex.withLock {
            try {
                procesarColaPendientesInterna(uid)
            } catch (e: Exception) {
                interpretarExcepcionFirebase(e)
            }
        }
    }

    private suspend fun procesarColaPendientesInterna(uid: String): ResultadoSync {
        val pendientes = dao.obtenerPendientesDeSync(uid)

        for (item in pendientes) {
            when (item.estadoSync) {
                EstadoSync.PENDIENTE_CREACION.name,
                EstadoSync.PENDIENTE_ACTUALIZACION.name,
                EstadoSync.ERROR_SYNC.name -> {
                    try {
                        firestoreDataSource.guardarOActualizar(uid, item)
                        dao.actualizar(item.copy(estadoSync = EstadoSync.SINCRONIZADO.name))
                    } catch (e: Exception) {
                        dao.actualizar(item.copy(estadoSync = EstadoSync.ERROR_SYNC.name))
                        return interpretarExcepcionFirebase(e)
                    }
                }

                EstadoSync.PENDIENTE_ELIMINACION.name -> {
                    try {
                        firestoreDataSource.eliminar(uid, item.id)
                        dao.eliminar(item)
                    } catch (e: Exception) {
                        return interpretarExcepcionFirebase(e)
                    }
                }
            }
        }
        return ResultadoSync.Exito
    }

    private fun interpretarExcepcionFirebase(e: Exception): ResultadoSync.ErrorControlado {
        val mensaje = when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Acceso denegado por reglas de seguridad de Firestore."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Servicio cloud temporalmente no disponible. Tus cambios siguen guardados localmente."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Tu sesión expiró. Volvé a iniciar sesión para sincronizar."
                else ->
                    "Error de Firestore (${e.code.name}). Datos conservados en el dispositivo."
            }
            is FirebaseNetworkException, is IOException ->
                "Sin conexión a internet. Modo offline activo: se sincronizará al recuperar la red."
            else ->
                "No se pudo sincronizar (${e.localizedMessage ?: "error desconocido"}). Datos locales a salvo."
        }
        return ResultadoSync.ErrorControlado(mensaje)
    }
}