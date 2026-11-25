package com.example.recordatoriosapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // 1. Preferencias persistentes (DataStore)
    val preferenciasRepository: PreferenciasRepository by lazy {
        PreferenciasRepository(appContext.dataStore)
    }

    // 2. Base de datos local (Room)
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    // 3. Fuentes Cloud (Firebase Authentication + Cloud Firestore)
    val authDataSource: FirebaseAuthDataSource by lazy {
        FirebaseAuthDataSource()
    }

    private val firestoreDataSource: FirestoreDataSource by lazy {
        FirestoreDataSource()
    }

    // 4. Repositorio Local-First conectado a Cloud
    val recordatorioRepository: RecordatorioRepository by lazy {
        RecordatorioRepository(
            dao = database.recordatorioDao(),
            firestoreDataSource = firestoreDataSource,
            authDataSource = authDataSource
        )
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.authDataSource, container.recordatorioRepository) as T
            }
            modelClass.isAssignableFrom(ListadoViewModel::class.java) -> {
                ListadoViewModel(container.recordatorioRepository, container.preferenciasRepository) as T
            }
            modelClass.isAssignableFrom(CreacionViewModel::class.java) -> {
                CreacionViewModel(container.recordatorioRepository) as T
            }
            modelClass.isAssignableFrom(DetalleViewModel::class.java) -> {
                DetalleViewModel(container.recordatorioRepository) as T
            }
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
    }
}