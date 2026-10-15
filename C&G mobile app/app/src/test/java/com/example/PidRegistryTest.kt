package com.example

import com.example.platform.obd.PidRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PidRegistryTest {

    @Test
    fun testRpmDecoding() {
        val def = PidRegistry.ALL_DEFINITIONS[PidRegistry.PID_ENGINE_RPM]
        assertNotNull(def)
        // 0x1A = 26, 0xF8 = 248 -> ((26 * 256) + 248) / 4 = 6904 / 4 = 1726.0 rpm
        val bytes = listOf(0x1A, 0xF8)
        val decoded = def!!.decode(bytes)
        assertNotNull(decoded)
        assertEquals(1726.0, decoded!!, 0.01)
    }

    @Test
    fun testSpeedDecoding() {
        val def = PidRegistry.ALL_DEFINITIONS[PidRegistry.PID_VEHICLE_SPEED]
        assertNotNull(def)
        // 0x48 = 72 km/h
        val bytes = listOf(0x48)
        val decoded = def!!.decode(bytes)
        assertNotNull(decoded)
        assertEquals(72.0, decoded!!, 0.01)
    }

    @Test
    fun testCoolantTempDecoding() {
        val def = PidRegistry.ALL_DEFINITIONS[PidRegistry.PID_COOLANT_TEMP]
        assertNotNull(def)
        // A - 40: 0x82 = 130 -> 130 - 40 = 90.0 °C
        val bytes = listOf(0x82)
        val decoded = def!!.decode(bytes)
        assertNotNull(decoded)
        assertEquals(90.0, decoded!!, 0.01)
    }

    @Test
    fun testCalculatedEngineLoadDecoding() {
        val def = PidRegistry.ALL_DEFINITIONS[PidRegistry.PID_ENGINE_LOAD]
        assertNotNull(def)
        // (A * 100) / 255: 0x80 = 128 -> (128 * 100) / 255 = 50.196%
        val bytes = listOf(0x80)
        val decoded = def!!.decode(bytes)
        assertNotNull(decoded)
        assertEquals(50.196, decoded!!, 0.01)
    }

    @Test
    fun testModuleVoltageDecoding() {
        val def = PidRegistry.ALL_DEFINITIONS[PidRegistry.PID_MODULE_VOLTAGE]
        assertNotNull(def)
        // ((A * 256) + B) / 1000.0: 0x37 = 55, 0x78 = 120 -> ((55 * 256) + 120) / 1000 = 14200 / 1000 = 14.2V
        val bytes = listOf(0x37, 0x78)
        val decoded = def!!.decode(bytes)
        assertNotNull(decoded)
        assertEquals(14.2, decoded!!, 0.01)
    }

    @Test
    fun testSupportedPidsBitmapParsing() {
        // Bitmap bytes: BE 3E B8 11
        // 0xBE = 1011 1110 -> PIDs 1, 3, 4, 5, 6, 7
        val bytes = listOf(0xBE, 0x3E, 0xB8, 0x11)
        val supported = PidRegistry.parseSupportedBitmap(bytes, baseRange = 0x00)

        // 0104 (Calculated load), 0105 (Coolant), 010C (RPM), 010D (Speed) should be present
        assertTrue(supported.contains(PidRegistry.PID_ENGINE_LOAD))
        assertTrue(supported.contains(PidRegistry.PID_COOLANT_TEMP))
        assertTrue(supported.contains(PidRegistry.PID_ENGINE_RPM))
        assertTrue(supported.contains(PidRegistry.PID_VEHICLE_SPEED))

        // Last bit of 0x11 is 1 -> indicates next range (0120) is supported!
        assertTrue(PidRegistry.hasNextRange(bytes))
    }

    @Test
    fun testBitmapHasNextRangeFalse() {
        val bytes = listOf(0x80, 0x00, 0x00, 0x00)
        assertFalse(PidRegistry.hasNextRange(bytes))
    }
}
