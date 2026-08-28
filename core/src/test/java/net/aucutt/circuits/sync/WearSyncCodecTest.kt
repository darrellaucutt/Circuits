package net.aucutt.circuits.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class WearSyncCodecTest {

    @Test
    fun timerState_roundTrip() {
        val state = SyncTimerState(
            phase = "Work",
            remainingSeconds = 45,
            currentRound = 3,
            isPaused = false,
            intervalMinutes = 2,
            cooldownMinutes = 1,
            repeats = 10,
            circuitName = "Hills",
            updatedAt = 999L,
        )

        val decoded = WearSyncCodec.timerStateFromJson(WearSyncCodec.timerStateToJson(state))

        assertEquals(state, decoded)
    }

    @Test
    fun command_roundTrip() {
        val command = WearCommand(WearCommandAction.PAUSE, timestamp = 123L)

        val decoded = WearSyncCodec.commandFromJson(WearSyncCodec.commandToJson(command))

        assertEquals(command, decoded)
    }
}
