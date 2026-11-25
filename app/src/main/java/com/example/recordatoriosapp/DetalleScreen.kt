package com.example.recordatoriosapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    id: String,
    viewModel: DetalleViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eliminado by viewModel.eliminado.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = id) {
        viewModel.cargarDetalle(id)
    }

    LaunchedEffect(key1 = eliminado) {
        if (eliminado) {
            onVolver()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Recordatorio") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DetalleUiState.Cargando -> EstadoPantalla(
                cargando = true,
                modifier = Modifier.padding(innerPadding)
            )
            is DetalleUiState.Error -> EstadoPantalla(
                mensajeError = state.mensaje,
                modifier = Modifier.padding(innerPadding)
            )
            is DetalleUiState.Exito -> {
                val r = state.recordatorio
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    Text(text = r.titulo, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Categoría: ${r.categoria}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Prioridad: ${r.prioridad}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Estado: ${r.estado.descripcion}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Estado de sincronización: ${r.estadoSync.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = r.descripcion ?: "Sin descripción adicional.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Cambiar estado:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cambiarEstado(r, EstadoRecordatorio.Pendiente) },
                            enabled = r.estado !is EstadoRecordatorio.Pendiente
                        ) {
                            Text("Pendiente")
                        }
                        OutlinedButton(
                            onClick = { viewModel.cambiarEstado(r, EstadoRecordatorio.EnProceso) },
                            enabled = r.estado !is EstadoRecordatorio.EnProceso
                        ) {
                            Text("En Proceso")
                        }
                        OutlinedButton(
                            onClick = { viewModel.cambiarEstado(r, EstadoRecordatorio.Completado) },
                            enabled = r.estado !is EstadoRecordatorio.Completado
                        ) {
                            Text("Completado")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.eliminarRecordatorio(r.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar Recordatorio")
                    }
                }
            }
        }
    }
}