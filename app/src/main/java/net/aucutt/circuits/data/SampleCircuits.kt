package net.aucutt.circuits.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object SampleCircuits {
    val presets = listOf(
        CircuitEntity(
            name = "Hills",
            intervalMinutes = 2,
            cooldownMinutes = 1,
            repeats = 10,
        ),
        CircuitEntity(
            name = "Cruise",
            intervalMinutes = 5,
            cooldownMinutes = 2,
            repeats = 8,
        ),
    )

    suspend fun insertPresetsIfMissing(connection: SQLiteConnection) {
        presets.forEach { preset ->
            connection.execSQL(
                """
                INSERT INTO circuits (name, intervalMinutes, cooldownMinutes, repeats)
                SELECT '${preset.name}', ${preset.intervalMinutes}, ${preset.cooldownMinutes}, ${preset.repeats}
                WHERE NOT EXISTS (SELECT 1 FROM circuits WHERE name = '${preset.name}')
                """.trimIndent(),
            )
        }
    }
}
