package net.aucutt.circuits.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import net.aucutt.circuits.ui.timer.TimerConfig

@Entity(tableName = "circuits")
data class CircuitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val intervalMinutes: Int,
    val cooldownMinutes: Int,
    val repeats: Int,
) {
    fun toConfig() = TimerConfig(
        intervalMinutes = intervalMinutes,
        cooldownMinutes = cooldownMinutes,
        repeats = repeats,
    )

    companion object {
        fun from(name: String, config: TimerConfig, id: Long = 0) = CircuitEntity(
            id = id,
            name = name,
            intervalMinutes = config.intervalMinutes,
            cooldownMinutes = config.cooldownMinutes,
            repeats = config.repeats,
        )
    }
}
