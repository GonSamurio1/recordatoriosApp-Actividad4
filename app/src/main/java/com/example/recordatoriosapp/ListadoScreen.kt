package com.example.recordatoriosapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    viewModel: ListadoViewModel,
    emailUsuario: String,
    resultadoPruebaSeguridad: String?,
    onProbarSeguridad: () -> Unit,
    onLimpiarPruebaSeguridad: () -> Unit,
    onCerrarSesion: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    onCrearRecordatorio: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferencias by viewModel.preferencias.collectAsStateWithLifecycle()
    val mensajeSync by viewModel.mensajeSync.collectAsStateWithLifecycle()
    val sincronizando by viewModel.sincronizando.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Recordatorios") },
                actions = {
                    TextButton(
                        onClick = { viewModel.sincronizar(descargarRemotos = true) },
                        enabled = !sincronizando
                    ) {
                        Text(if (sincronizando) "Sync..." else "Sincronizar")
                    }
                    TextButton(onClick = onCerrarSesion) {
                        Text("Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Información de sesión activa + botón de verificación de reglas de Firestore (Consigna 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sesión: $emailUsuario",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onProbarSeguridad) {
                    Text("Probar reglas", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Resultado de la prueba de seguridad contra uid ajeno (Consigna 3)
            resultadoPruebaSeguridad?.let { resultado ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = resultado,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onLimpiarPruebaSeguridad) {
                            Text("OK")
                        }
                    }
                }
            }

            // Barra de preferencias persistentes (DataStore)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferencias.temaOscuro,
                        onClick = { viewModel.cambiarTemaOscuro(!preferencias.temaOscuro) },
                        label = { Text(if (preferencias.temaOscuro) "Tema: Oscuro" else "Tema: Claro") }
                    )
                    FilterChip(
                        selected = preferencias.mostrarCompletados,
                        onClick = { viewModel.cambiarMostrarCompletados(!preferencias.mostrarCompletados) },
                        label = { Text(if (preferencias.mostrarCompletados) "Ver completados" else "Ocultar completados") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Orden:", style = MaterialTheme.typography.labelMedium)
                    CriterioOrden.entries.forEach { criterio ->
                        FilterChip(
                            selected = preferencias.ordenamiento == criterio,
                            onClick = { viewModel.cambiarOrdenamiento(criterio) },
                            label = { Text(criterio.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            // Aviso controlado ante error de red / modo offline
            mensajeSync?.let { aviso ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = aviso,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.limpiarMensajeSync() }) {
                            Text("OK")
                        }
                    }
                }
            }

            // Contenido principal reactivo desde Room
            when (val state = uiState) {
                is ListadoUiState.Cargando -> EstadoPantalla(
                    cargando = true,
                    modifier = Modifier.fillMaxSize()
                )
                is ListadoUiState.Vacio -> EstadoPantalla(
                    mensajeVacio = "No tenés recordatorios visibles.",
                    onCrear = onCrearRecordatorio,
                    modifier = Modifier.fillMaxSize()
                )
                is ListadoUiState.Error -> EstadoPantalla(
                    mensajeError = state.mensaje,
                    onReintentar = { viewModel.sincronizar(descargarRemotos = true) },
                    modifier = Modifier.fillMaxSize()
                )
                is ListadoUiState.Exito -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.recordatorios, key = { it.id }) { recordatorio ->
                        Column {
                            RecordatorioItem(
                                recordatorio = recordatorio,
                                onClick = { onRecordatorioClick(recordatorio.id) }
                            )
                            AssistChip(
                                onClick = { onRecordatorioClick(recordatorio.id) },
                                label = {
                                    val etiquetaSync = when (recordatorio.estadoSync) {
                                        EstadoSync.SINCRONIZADO -> "Sincronizado"
                                        EstadoSync.PENDIENTE_CREACION -> "Pendiente de envío (offline)"
                                        EstadoSync.PENDIENTE_ACTUALIZACION -> "Actualización pendiente"
                                        EstadoSync.PENDIENTE_ELIMINACION -> "Eliminación pendiente"
                                        EstadoSync.ERROR_SYNC -> "Error de sync - reintentar"
                                    }
                                    Text("Sync: $etiquetaSync", style = MaterialTheme.typography.labelSmall)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}