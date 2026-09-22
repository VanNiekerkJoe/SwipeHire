package com.swipehire.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileUtilsTest {
    @Test
    fun `profile strength counts CV when required`() {
        assertEquals(50, calculateProfileStrength(listOf("Name", "", "Year"), hasCv = false, cvRequired = true))
        assertEquals(75, calculateProfileStrength(listOf("Name", "", "Year"), hasCv = true, cvRequired = true))
    }

    @Test
    fun `skill matching ignores case and keeps candidate labels`() {
        assertEquals(listOf("Kotlin", "REST APIs"), matchingSkills(listOf("Kotlin", "SQL", "REST APIs"), listOf("kotlin", "rest apis")))
    }
}
