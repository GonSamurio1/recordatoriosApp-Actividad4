# RecordatoriosApp

Aplicación móvil nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** que permite gestionar recordatorios personales con persistencia híbrida (*offline-first* mediante Room + sincronización en la nube por usuario con Cloud Firestore) y autenticación con Firebase Authentication.

---

## Características principales

* **Autenticación de usuarios:** Registro, inicio y cierre de sesión con correo electrónico y contraseña mediante **Firebase Authentication**.
* **Persistencia híbrida y sincronización en tiempo real:** Almacenamiento local con **Room** para funcionamiento sin conexión y respaldo automático en **Cloud Firestore** aislado por usuario (`users/{uid}/reminders`).
* **Gestión completa de recordatorios (CRUD):** Creación, edición, consulta de detalle, filtrado por estado y eliminación con actualización reactiva de la interfaz.
* **Preferencias de usuario:** Configuración persistente mediante **Jetpack DataStore**.
* **Arquitectura limpia:** Separación en capas de UI (Jetpack Compose + ViewModels con `StateFlow`), Repositorio, Fuentes de Datos locales/remotas y Mappers DTO/Entity.

---

## Stack tecnológico

* **Lenguaje:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Arquitectura:** MVVM + Repository Pattern
* **Backend & Cloud:** Firebase Authentication, Cloud Firestore
* **Persistencia local:** Room Database, Jetpack DataStore
* **Build & Optimización:** Gradle (Kotlin DSL), Product Flavors, R8 / ProGuard

---

## Seguridad en Cloud Firestore

Cada usuario autenticado únicamente tiene acceso de lectura y escritura sobre su propia subcolección de recordatorios, bloqueando cualquier acceso anónimo o cruzado entre cuentas:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/reminders/{reminderId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
