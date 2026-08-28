package net.aucutt.circuits.sync

object WearSyncPaths {
    const val TIMER_STATE = "/timer_state"
    const val COMMAND = "/command"
}

enum class WearCommandAction {
    PAUSE,
    RESUME,
    STOP,
}

data class WearCommand(
    val action: WearCommandAction,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Mirror of the phone timer state pushed to the watch. */
data class SyncTimerState(
    val phase: String,
    val remainingSeconds: Int,
    val currentRound: Int,
    val isPaused: Boolean,
    val intervalMinutes: Int,
    val cooldownMinutes: Int,
    val repeats: Int,
    val circuitName: String,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isRunning: Boolean
        get() = phase == "PreWorkout" || phase == "Work" || phase == "Cooldown"
}
