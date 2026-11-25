package com.example.recordatoriosapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResultado {
    data class Exito(val user: FirebaseUser) : AuthResultado()
    data class Error(val mensaje: String) : AuthResultado()
}

class FirebaseAuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    val uidActual: String?
        get() = auth.currentUser?.uid

    // Observa en tiempo real si hay sesión activa, cierre o invalidez de sesión
    fun observarEstadoAutenticacion(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun iniciarSesion(email: String, password: String): AuthResultado {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                AuthResultado.Exito(user)
            } else {
                AuthResultado.Error("No se pudo recuperar la sesión del usuario.")
            }
        } catch (e: Exception) {
            AuthResultado.Error(traducirErrorAuth(e))
        }
    }

    suspend fun registrarUsuario(email: String, password: String): AuthResultado {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                AuthResultado.Exito(user)
            } else {
                AuthResultado.Error("No se pudo completar el registro.")
            }
        } catch (e: Exception) {
            AuthResultado.Error(traducirErrorAuth(e))
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    private fun traducirErrorAuth(e: Exception): String {
        return when ((e as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Correo o contraseña incorrectos."
            "ERROR_USER_NOT_FOUND" -> "No existe una cuenta registrada con ese correo."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya está registrado. Probá iniciar sesión."
            "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres."
            "ERROR_USER_DISABLED" -> "Esta cuenta fue deshabilitada."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión a internet para verificar la cuenta."
            else -> e.localizedMessage ?: "Error inesperado de autenticación."
        }
    }
}