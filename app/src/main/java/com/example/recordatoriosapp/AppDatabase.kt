package com.example.recordatoriosapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecordatorioEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recordatorios_nueva` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `titulo` TEXT NOT NULL,
                        `descripcion` TEXT,
                        `categoria` TEXT NOT NULL,
                        `prioridad` TEXT NOT NULL,
                        `estado` TEXT NOT NULL,
                        `fechaCreacion` INTEGER NOT NULL,
                        `estadoSync` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recordatorios_nueva` (
                        `id`, `userId`, `titulo`, `descripcion`, `categoria`,
                        `prioridad`, `estado`, `fechaCreacion`, `estadoSync`
                    )
                    SELECT
                        CAST(`id` AS TEXT), 'legacy_local', `titulo`, `descripcion`, `categoria`,
                        `prioridad`, `estado`, `fechaCreacion`, `estadoSync`
                    FROM `recordatorios`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `recordatorios`")
                db.execSQL("ALTER TABLE `recordatorios_nueva` RENAME TO `recordatorios`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recordatorios_local_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}