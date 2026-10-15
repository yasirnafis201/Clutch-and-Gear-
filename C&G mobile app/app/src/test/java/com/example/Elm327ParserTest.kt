package com.example

import com.example.domain.models.DtcType
import com.example.platform.obd.Elm327Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327ParserTest {

    @Test
    fun testCleanResponse() {
        val raw = "41 0C 1A F8\r\n>"
        val cleaned = Elm327Parser.cleanResponse(raw)
        assertEquals("41 0C 1A F8", cleaned)
    }

    @Test
    fun testCheckError() {
        assertNotNull(Elm327Parser.checkError("NO DATA"))
        assertNotNull(Elm327Parser.checkError("CAN ERROR"))
        assertNotNull(Elm327Parser.checkError("BUS INIT: ... ERROR"))
        assertNotNull(Elm327Parser.checkError("?"))
        assertNull(Elm327Parser.checkError("41 0C 1A F8"))
    }

    @Test
    fun testParseMode01DataBytesWithSpaces() {
        val raw = "41 0C 1A F8\r>"
        val bytes = Elm327Parser.parseMode01DataBytes(raw, "0C")
        assertEquals(listOf(0x1A, 0xF8), bytes)
    }

    @Test
    fun testParseMode01DataBytesContinuousHex() {
        val raw = "410C1AF8\r>"
        val bytes = Elm327Parser.parseMode01DataBytes(raw, "0C")
        assertEquals(listOf(0x1A, 0xF8), bytes)
    }

    @Test
    fun testParseMode01DataBytesEchoedCommand() {
        val raw = "010C\r41 0C 1A F8\r>"
        val bytes = Elm327Parser.parseMode01DataBytes(raw, "0C")
        assertEquals(listOf(0x1A, 0xF8), bytes)
    }

    @Test
    fun testParseDtcResponse() {
        // "43 01 15 03 00 00 00" -> P0115 and P0300
        val raw = "43 01 15 03 00 00 00\r>"
        val codes = Elm327Parser.parseDiagnosticTroubleCodes(raw, DtcType.STORED)
        assertEquals(2, codes.size)

        assertEquals("P0115", codes[0].code)
        assertEquals("Powertrain", codes[0].category)
        assertTrue(codes[0].description.contains("Coolant"))

        assertEquals("P0300", codes[1].code)
        assertEquals("Powertrain", codes[1].category)
        assertTrue(codes[1].description.contains("Misfire"))
    }

    @Test
    fun testParseDtcNoData() {
        val raw = "NO DATA\r>"
        val codes = Elm327Parser.parseDiagnosticTroubleCodes(raw, DtcType.STORED)
        assertTrue(codes.isEmpty())
    }
}
