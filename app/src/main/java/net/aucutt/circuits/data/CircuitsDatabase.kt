package net.aucutt.circuits.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [CircuitEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CircuitsDatabase : RoomDatabase() {
    abstract fun circuitDao(): CircuitDao

    companion object {
        @Volatile
        private var instance: CircuitsDatabase? = null

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
                .build()
        }
    }
}
