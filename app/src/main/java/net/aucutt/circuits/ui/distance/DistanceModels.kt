package net.aucutt.circuits.ui.distance

data class DistanceConfig(
    val halfMiles: Int = DistanceMiles.DEFAULT_HALF_MILES,
    val cooldownMinutes: Int = 1,
    val repeats: Int = 8,
) {
    val targetMeters: Double get() = DistanceMiles.toMeters(halfMiles)
    val cooldownSeconds: Int get() = cooldownMinutes * 60

    fun withHalfMiles(value: Int) = copy(halfMiles = value.coerceAtLeast(DistanceMiles.MIN_HALF_MILES))
    fun withCooldown(minutes: Int) = copy(cooldownMinutes = minutes.coerceAtLeast(0))
    fun withRepeats(count: Int) = copy(repeats = count.coerceAtLeast(1))
}

enum class DistancePhase {
    Idle,
    PreWorkout,
    Work,
    Cooldown,
    Finished,
}

data class DistanceUiState(
    val config: DistanceConfig = DistanceConfig(),
    val phase: DistancePhase = DistancePhase.Idle,
    val distanceMeters: Double = 0.0,
    val remainingSeconds: Int = 0,
    val currentRound: Int = 0,
    val isPaused: Boolean = false,
    val hasGpsFix: Boolean = false,
) {
    val isRunning: Boolean
        get() = phase == DistancePhase.PreWorkout ||
            phase == DistancePhase.Work ||
            phase == DistancePhase.Cooldown

    val remainingWorkMeters: Double
        get() = (config.targetMeters - distanceMeters).coerceAtLeast(0.0)
}
