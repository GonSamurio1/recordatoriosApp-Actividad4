package com.example.recordatoriosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatoriosapp.ui.theme.RecordatoriosAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appContainer = remember { AppContainer(context) }
            val factory = remember(appContainer) { AppViewModelFactory(appContainer) }
            val preferencias by appContainer.preferenciasRepository.preferenciasFlow
                .collectAsStateWithLifecycle(initialValue = PreferenciasApp())

            val authViewModel: AuthViewModel = viewModel(factory = factory)
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val procesandoAuth by authViewModel.procesando.collectAsStateWithLifecycle()
            val resultadoPrueba by authViewModel.resultadoPruebaSeguridad.collectAsStateWithLifecycle()

            RecordatoriosAppTheme(darkTheme = preferencias.temaOscuro) {
                when (val state = authState) {
                    is AuthUiState.Cargando -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AuthUiState.NoAutenticado -> {
                        AuthScreen(
                            mensajeError = state.mensajeError,
                            procesando = procesandoAuth,
                            onIniciarSesion = { email, pass ->
                                authViewModel.iniciarSesion(email, pass)
                            },
                            onRegistrar = { email, pass ->
                                authViewModel.registrar(email, pass)
                            }
                        )
                    }

                    is AuthUiState.Autenticado -> {
                        AppNavigation(
                            appContainer = appContainer,
                            uidUsuario = state.uid,
                            emailUsuario = state.email,
                            resultadoPruebaSeguridad = resultadoPrueba,
                            onProbarSeguridad = { authViewModel.probarAccesoOtroUsuario() },
                            onLimpiarPruebaSeguridad = { authViewModel.limpiarResultadoPrueba() },
                            onCerrarSesion = { authViewModel.cerrarSesion() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    appContainer: AppContainer,
    uidUsuario: String,
    emailUsuario: String,
    resultadoPruebaSeguridad: String?,
    onProbarSeguridad: () -> Unit,
    onLimpiarPruebaSeguridad: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val navController = rememberNavController()
    val factory = remember(appContainer) { AppViewModelFactory(appContainer) }

    NavHost(
        navController = navController,
        startDestination = "listado"
    ) {
        composable(route = "listado") {
            val vm: ListadoViewModel = viewModel(factory = factory)

            // Al cambiar de usuario autenticado, sincronizamos los datos de esa cuenta
            LaunchedEffect(uidUsuario) {
                vm.sincronizar(descargarRemotos = true)
            }

            ListadoScreen(
                viewModel = vm,
                emailUsuario = emailUsuario,
                resultadoPruebaSeguridad = resultadoPruebaSeguridad,
                onProbarSeguridad = onProbarSeguridad,
                onLimpiarPruebaSeguridad = onLimpiarPruebaSeguridad,
                onCerrarSesion = onCerrarSesion,
                onRecordatorioClick = { id -> navController.navigate(route = "detalle/$id") },
                onCrearRecordatorio = { navController.navigate(route = "creacion") }
            )
        }

        composable(route = "creacion") {
            val vm: CreacionViewModel = viewModel(factory = factory)
            CreacionScreen(
                viewModel = vm,
                onGuardarExito = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable(
            route = "detalle/{id}",
            arguments = listOf(navArgument(name = "id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val vm: DetalleViewModel = viewModel(factory = factory)
            DetalleScreen(
                id = id,
                viewModel = vm,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}