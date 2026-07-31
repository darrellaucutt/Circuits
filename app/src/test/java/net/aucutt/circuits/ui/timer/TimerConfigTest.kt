package net.aucutt.circuits.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerConfigTest {

    @Test
    fun withInterval_clampsToMinimumOfOne() {
        assertEquals(1, TimerConfig().withInterval(0).intervalMinutes)
        assertEquals(1, TimerConfig().withInterval(-5).intervalMinutes)
        assertEquals(3, TimerConfig().withInterval(3).intervalMinutes)
    }

    @Test
    fun withCooldown_clampsToMinimumOfZero() {
        assertEquals(0, TimerConfig().withCooldown(-1).cooldownMinutes)
        assertEquals(2, TimerConfig().withCooldown(2).cooldownMinutes)
    }

    @Test
    fun withRepeats_clampsToMinimumOfOne() {
        assertEquals(1, TimerConfig().withRepeats(0).repeats)
        assertEquals(4, TimerConfig().withRepeats(4).repeats)
    }

    @Test
    fun intervalAndCooldownSeconds_deriveFromMinutes() {
        val config = TimerConfig(intervalMinutes = 2, cooldownMinutes = 1, repeats = 1)
        assertEquals(120, config.intervalSeconds)
        assertEquals(60, config.cooldownSeconds)
    }
}
