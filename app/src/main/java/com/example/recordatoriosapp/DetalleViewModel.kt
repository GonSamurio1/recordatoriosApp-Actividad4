package com.example.recordatoriosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetalleViewModel(
    private val repository: RecordatorioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private val _eliminado = MutableStateFlow(false)
    val eliminado: StateFlow<Boolean> = _eliminado.asStateFlow()

    fun cargarDetalle(id: String) {
        viewModelScope.launch {
            _uiState.value = DetalleUiState.Cargando
            val recordatorio = repository.obtenerPorId(id)
            if (recordatorio != null) {
                _uiState.value = DetalleUiState.Exito(recordatorio)
            } else {
                _uiState.value = DetalleUiState.Error("No se encontró el recordatorio seleccionado")
            }
        }
    }

    fun cambiarEstado(recordatorio: Recordatorio, nuevoEstado: EstadoRecordatorio) {
        viewModelScope.launch {
            val actualizado = recordatorio.copy(estado = nuevoEstado)
            repository.actualizarRecordatorio(actualizado)
            // Recargamos desde Room para reflejar el nuevo estado y estadoSync actualizado
            val recargado = repository.obtenerPorId(recordatorio.id) ?: actualizado
            _uiState.value = DetalleUiState.Exito(recargado)
        }
    }

    fun eliminarRecordatorio(id: String) {
        viewModelScope.launch {
            repository.eliminarRecordatorio(id)
            _eliminado.value = true
        }
    }
}