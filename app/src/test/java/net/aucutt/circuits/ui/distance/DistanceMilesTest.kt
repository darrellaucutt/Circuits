package net.aucutt.circuits.ui.distance

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceMilesTest {

    @Test
    fun format_wholeMiles_omitsDecimal() {
        assertEquals("2", DistanceMiles.format(4))
        assertEquals("1", DistanceMiles.format(2))
    }

    @Test
    fun format_halfMiles_showsDecimal() {
        assertEquals("1.5", DistanceMiles.format(3))
        assertEquals("0.5", DistanceMiles.format(1))
        assertEquals("2.5", DistanceMiles.format(5))
    }

    @Test
    fun formatForTts_usesNaturalPhrasing() {
        assertEquals("Half mile", DistanceMiles.formatForTts(1))
        assertEquals("One mile", DistanceMiles.formatForTts(2))
        assertEquals("1.5 miles", DistanceMiles.formatForTts(3))
    }

    @Test
    fun toMeters_convertsHalfMiles() {
        assertEquals(DistanceMiles.METERS_PER_MILE, DistanceMiles.toMeters(2), 0.001)
    }

    @Test
    fun formatDecimal_showsTenthsWhenNeeded() {
        assertEquals("1.2", DistanceMiles.formatDecimal(1.23))
        assertEquals("2", DistanceMiles.formatDecimal(2.0))
    }
}
