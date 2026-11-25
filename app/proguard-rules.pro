# Mantener entidades de Room y modelos de dominio
-keep class com.example.recordatoriosapp.** { *; }

# Evitar advertencias de clases opcionales de Firebase, Retrofit y OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.firebase.**
-dontwarn javax.annotation.**