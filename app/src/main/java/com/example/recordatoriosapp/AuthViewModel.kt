package com.example.recordatoriosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Cargando : AuthUiState()
    data class NoAutenticado(val mensajeError: String? = null) : AuthUiState()
    data class Autenticado(val uid: String, val email: String) : AuthUiState()
}

class AuthViewModel(
    private val authDataSource: FirebaseAuthDataSource,
    private val repository: RecordatorioRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Cargando)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando: StateFlow<Boolean> = _procesando.asStateFlow()

    private val _resultadoPruebaSeguridad = MutableStateFlow<String?>(null)
    val resultadoPruebaSeguridad: StateFlow<String?> = _resultadoPruebaSeguridad.asStateFlow()

    init {
        viewModelScope.launch {
            authDataSource.observarEstadoAutenticacion().collect { user ->
                if (user != null) {
                    _authState.value = AuthUiState.Autenticado(
                        uid = user.uid,
                        email = user.email ?: "Usuario autenticado"
                    )
                } else {
                    _authState.value = AuthUiState.NoAutenticado()
                }
            }
        }
    }

    fun iniciarSesion(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthUiState.NoAutenticado("Completá correo y contraseña.")
            return
        }
        viewModelScope.launch {
            _procesando.value = true
            when (val res = authDataSource.iniciarSesion(email, password)) {
                is AuthResultado.Exito -> {
                    // El listener de observarEstadoAutenticacion actualiza a Autenticado automáticamente
                }
                is AuthResultado.Error -> {
                    _authState.value = AuthUiState.NoAutenticado(res.mensaje)
                }
            }
            _procesando.value = false
        }
    }

    fun registrar(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _authState.value = AuthUiState.NoAutenticado("Ingresá un correo válido y contraseña de al menos 6 caracteres.")
            return
        }
        viewModelScope.launch {
            _procesando.value = true
            when (val res = authDataSource.registrarUsuario(email, password)) {
                is AuthResultado.Exito -> {
                    // El listener actualiza a Autenticado automáticamente
                }
                is AuthResultado.Error -> {
                    _authState.value = AuthUiState.NoAutenticado(res.mensaje)
                }
            }
            _procesando.value = false
        }
    }

    fun cerrarSesion() {
        _resultadoPruebaSeguridad.value = null
        authDataSource.cerrarSesion()
    }

    // Prueba explícita para Consigna 3: intenta leer documentos de un uid ajeno
    fun probarAccesoOtroUsuario() {
        viewModelScope.launch {
            val resultado = repository.verificarAislamientoSeguridad("uid_usuario_ajeno_prueba")
            _resultadoPruebaSeguridad.value = resultado
        }
    }

    fun limpiarResultadoPrueba() {
        _resultadoPruebaSeguridad.value = null
    }
}