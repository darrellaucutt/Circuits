package net.aucutt.circuits.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.migration.Migration
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [CircuitEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CircuitsDatabase : RoomDatabase() {
    abstract fun circuitDao(): CircuitDao

    companion object {
        @Volatile
        private var instance: CircuitsDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                SampleCircuits.insertPresetsIfMissing(connection)
            }
        }

        private val sampleCircuitsCallback = object : RoomDatabase.Callback() {
            override suspend fun onCreate(connection: SQLiteConnection) {
                super.onCreate(connection)
                SampleCircuits.insertPresetsIfMissing(connection)
            }
        }

        fun getInstance(context: Context): CircuitsDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): CircuitsDatabase {
            return Room.databaseBuilder<CircuitsDatabase>(
                context = context,
                name = "circuits.db",
            )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MIGRATION_1_2)
                .addCallback(sampleCircuitsCallback)
                .build()
        }
    }
}
