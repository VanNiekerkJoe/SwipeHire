package com.swipehire.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {
    @Test
    fun `same coordinate has zero distance`() {
        assertEquals(0.0, distanceKm(-26.1076, 28.0567, -26.1076, 28.0567), 0.0001)
    }

    @Test
    fun `Johannesburg to Pretoria is a plausible distance`() {
        val result = distanceKm(-26.2041, 28.0473, -25.7479, 28.2293)
        assertTrue(result in 50.0..60.0)
    }

    @Test
    fun `distance formatting handles metres and kilometres`() {
        assertEquals("750 m", formatDistance(0.75))
        assertEquals("3.2 km", formatDistance(3.24))
    }
}
