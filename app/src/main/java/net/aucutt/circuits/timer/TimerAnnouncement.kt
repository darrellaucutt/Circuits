package net.aucutt.circuits.timer

sealed interface TimerAnnouncement {
    data object PreWorkout : TimerAnnouncement
    data class WorkoutStart(val round: Int) : TimerAnnouncement
    data class Work(val round: Int) : TimerAnnouncement
    data object Cooldown : TimerAnnouncement
    data object Complete : TimerAnnouncement
}
