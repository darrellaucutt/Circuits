package net.aucutt.circuits.ui.distance

import kotlin.math.roundToInt

object DistanceMiles {
    const val DEFAULT_HALF_MILES = 4
    const val STEP_HALF_MILES = 1
    const val MIN_HALF_MILES = 1

    const val METERS_PER_MILE = 1609.344
    val METERS_PER_HALF_MILE: Double = METERS_PER_MILE / 2.0

    fun toMeters(halfMiles: Int): Double = halfMiles * METERS_PER_HALF_MILE

    fun format(halfMiles: Int): String {
        val whole = halfMiles / 2
        return if (halfMiles % 2 == 0) whole.toString() else "$whole.5"
    }

    fun formatMeters(meters: Double): String {
        val halfMiles = (meters / METERS_PER_HALF_MILE).roundToInt().coerceAtLeast(0)
        if (halfMiles == 0 && meters > 0.0) {
            return formatDecimal(meters / METERS_PER_MILE)
        }
        return format(halfMiles)
    }

    fun formatDecimal(miles: Double): String {
        val roundedTenths = (miles * 10.0).roundToInt() / 10.0
        return if (roundedTenths % 1.0 == 0.0) {
            roundedTenths.toInt().toString()
        } else {
            roundedTenths.toString()
        }
    }

    /** Natural phrasing for half-mile TTS markers (halfMiles: 1 = 0.5 mi, 2 = 1 mi, …). */
    fun formatForTts(halfMiles: Int): String {
        return when (format(halfMiles)) {
            "0.5" -> "Half mile"
            "1" -> "One mile"
            else -> "${format(halfMiles)} miles"
        }
    }
}
