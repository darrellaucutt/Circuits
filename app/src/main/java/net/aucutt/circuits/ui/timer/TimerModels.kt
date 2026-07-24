package net.aucutt.circuits.ui.timer

data class TimerConfig(
    val intervalMinutes: Int = 1,
    val cooldownMinutes: Int = 1,
    val repeats: Int = 8,
) {
    val intervalSeconds: Int get() = intervalMinutes * 60
    val cooldownSeconds: Int get() = cooldownMinutes * 60

    fun withInterval(minutes: Int) = copy(intervalMinutes = minutes.coerceAtLeast(1))
    fun withCooldown(minutes: Int) = copy(cooldownMinutes = minutes.coerceAtLeast(0))
    fun withRepeats(count: Int) = copy(repeats = count.coerceAtLeast(1))
}

enum class TimerPhase {
    Idle,
    Work,
    Cooldown,
    Finished,
}

data class TimerUiState(
    val config: TimerConfig = TimerConfig(),
    val phase: TimerPhase = TimerPhase.Idle,
    val remainingSeconds: Int = 0,
    val currentRound: Int = 0,
    val isPaused: Boolean = false,
) {
    val isRunning: Boolean
        get() = phase == TimerPhase.Work || phase == TimerPhase.Cooldown

    val isActiveSession: Boolean
        get() = phase != TimerPhase.Idle
}
