package net.aucutt.circuits.distance

sealed interface DistanceAnnouncement {
    data object PreWorkout : DistanceAnnouncement
    data class WorkStart(val round: Int) : DistanceAnnouncement
    data class HalfMileMarker(val halfMiles: Int) : DistanceAnnouncement
    data object WorkComplete : DistanceAnnouncement
    data object Cooldown : DistanceAnnouncement
    data object Complete : DistanceAnnouncement
}
