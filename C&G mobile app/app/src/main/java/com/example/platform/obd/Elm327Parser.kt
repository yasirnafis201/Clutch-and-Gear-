package com.example.platform.obd

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.DtcType

object Elm327Parser {

    private val ERROR_TOKENS = listOf(
        "NO DATA",
        "CAN ERROR",
        "BUS INIT: ... ERROR",
        "BUS INIT: ERROR",
        "UNABLE TO CONNECT",
        "BUS BUSY",
        "STOPPED",
        "FB ERROR",
        "BUFFER FULL",
        "ERR",
        "?"
    )

    /**
     * Cleans an ELM327 raw string response by stripping prompts, whitespace, and null characters.
     */
    fun cleanResponse(raw: String): String {
        return raw.replace(">", "")
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
    }

    /**
     * Checks if the response contains any known ELM327 error string.
     */
    fun checkError(cleaned: String): String? {
        val upper = cleaned.uppercase()
        for (token in ERROR_TOKENS) {
            if (upper.contains(token)) {
                return token
            }
        }
        return null
    }

    /**
     * Extracts byte integers from an OBD response string for Mode 01 (e.g., "41 0C 1A F8" -> [26, 248]).
     */
    fun parseMode01DataBytes(raw: String, expectedPidHex: String): List<Int> {
        val cleaned = cleanResponse(raw).uppercase()
        // Split by whitespace
        val tokens = cleaned.split(" ").filter { it.isNotBlank() }

        // Look for "41" followed by PID hex, or continuous string like "410C1AF8"
        val expectedHeader = "41" + expectedPidHex.uppercase()

        // Case 1: Separate tokens e.g. ["41", "0C", "1A", "F8"]
        for (i in 0 until tokens.size - 1) {
            if (tokens[i] == "41" && tokens[i + 1].equals(expectedPidHex, ignoreCase = true)) {
                val dataTokens = tokens.subList(i + 2, tokens.size)
                return dataTokens.mapNotNull { it.toIntOrNull(16) }
            }
        }

        // Case 2: Joined string e.g. "410C1AF8" or multi-lines
        val hexOnly = cleaned.replace(" ", "")
        val headerIdx = hexOnly.indexOf(expectedHeader)
        if (headerIdx >= 0) {
            val dataPart = hexOnly.substring(headerIdx + expectedHeader.length)
            val result = mutableListOf<Int>()
            var idx = 0
            while (idx + 1 < dataPart.length) {
                val byteHex = dataPart.substring(idx, idx + 2)
                val byteVal = byteHex.toIntOrNull(16)
                if (byteVal != null) {
                    result.add(byteVal)
                } else {
                    break
                }
                idx += 2
            }
            return result
        }

        return emptyList()
    }

    /**
     * Decodes standard Mode 03 / 07 / 0A Diagnostic Trouble Codes.
     * Response usually formatted like "43 01 15 03 00 00 00" or "4301150300"
     */
    fun parseDiagnosticTroubleCodes(raw: String, codeType: DtcType = DtcType.STORED): List<DiagnosticCode> {
        val cleaned = cleanResponse(raw).uppercase()
        val error = checkError(cleaned)
        if (error != null) return emptyList()

        val tokens = cleaned.split(" ").filter { it.isNotBlank() }
        val hexTokens = mutableListOf<String>()

        for (token in tokens) {
            if (token.length == 2 && token.all { it.isDigit() || it in 'A'..'F' }) {
                hexTokens.add(token)
            } else if (token.length > 2 && token.length % 2 == 0 && token.all { it.isDigit() || it in 'A'..'F' }) {
                // Split multi-byte tokens like "430115"
                for (i in 0 until token.length step 2) {
                    hexTokens.add(token.substring(i, i + 2))
                }
            }
        }

        // Locate response mode indicator: "43" for Mode 03, "47" for Mode 07, "4A" for Mode 0A
        var startIdx = -1
        for (i in hexTokens.indices) {
            if (hexTokens[i] in listOf("43", "47", "4A")) {
                startIdx = i + 1
                break
            }
        }

        if (startIdx == -1) return emptyList()

        val dtcList = mutableListOf<DiagnosticCode>()
        var i = startIdx
        while (i + 1 < hexTokens.size) {
            val byte1 = hexTokens[i].toIntOrNull(16) ?: break
            val byte2 = hexTokens[i + 1].toIntOrNull(16) ?: break

            if (byte1 == 0 && byte2 == 0) {
                // Padding / no code
                i += 2
                continue
            }

            val prefix = when ((byte1 shr 6) and 0x03) {
                0 -> "P" // Powertrain
                1 -> "C" // Chassis
                2 -> "B" // Body
                3 -> "U" // Network
                else -> "P"
            }

            val category = when (prefix) {
                "P" -> "Powertrain"
                "C" -> "Chassis"
                "B" -> "Body"
                "U" -> "Network"
                else -> "General"
            }

            val digit1 = (byte1 shr 4) and 0x03
            val digit2 = byte1 and 0x0F
            val digit3 = (byte2 shr 4) and 0x0F
            val digit4 = byte2 and 0x0F

            val codeStr = String.format("%s%X%X%X%X", prefix, digit1, digit2, digit3, digit4)

            // Look up description or generate standard explanation
            val desc = getStandardDtcDescription(codeStr)

            dtcList.add(
                DiagnosticCode(
                    code = codeStr,
                    category = category,
                    description = desc.first,
                    codeType = codeType,
                    severity = desc.second,
                    possibleCauses = desc.third,
                    recommendedActions = desc.fourth
                )
            )

            i += 2
        }

        return dtcList
    }

    private fun getStandardDtcDescription(code: String): Tuple4 {
        return when (code) {
            "P0115" -> Tuple4(
                "Engine Coolant Temperature Circuit Malfunction",
                DtcSeverity.HIGH,
                listOf("Faulty ECT sensor", "Loose wiring harness", "Corroded sensor connector", "Low coolant level"),
                listOf("Check coolant level safely", "Inspect ECT sensor connector for corrosion", "Perform multitester resistance check")
            )
            "P0300" -> Tuple4(
                "Random / Multiple Cylinder Misfire Detected",
                DtcSeverity.HIGH,
                listOf("Worn spark plugs", "Failing ignition coils", "Low fuel pressure", "Vacuum leak"),
                listOf("Inspect spark plug condition", "Test coil packs for resistance", "Check fuel pressure and vacuum lines")
            )
            "P0420" -> Tuple4(
                "Catalyst System Efficiency Below Threshold (Bank 1)",
                DtcSeverity.MEDIUM,
                listOf("Degraded catalytic converter", "Defective downstream oxygen sensor", "Exhaust manifold leak"),
                listOf("Verify upstream and downstream O2 sensor voltages", "Inspect exhaust pipe for physical leaks")
            )
            "P0171" -> Tuple4(
                "System Too Lean (Bank 1)",
                DtcSeverity.MEDIUM,
                listOf("Dirty MAF sensor", "Vacuum intake leak", "Clogged fuel filter", "Faulty PCV valve"),
                listOf("Clean MAF sensor with dedicated cleaner", "Inspect intake boot for cracks", "Check fuel rail pressure")
            )
            "P0500" -> Tuple4(
                "Vehicle Speed Sensor Malfunction",
                DtcSeverity.MEDIUM,
                listOf("Defective VSS sensor", "Damaged wheel speed harness", "Instrument cluster wiring"),
                listOf("Inspect VSS sensor wiring and output signal")
            )
            else -> Tuple4(
                "Standard OBD-II Diagnostic Fault Code",
                DtcSeverity.MEDIUM,
                listOf("Sensor signal out of calibration", "Circuit continuity fault"),
                listOf("Perform guided diagnostic test with service manual")
            )
        }
    }

    private data class Tuple4(
        val first: String,
        val second: DtcSeverity,
        val third: List<String>,
        val fourth: List<String>
    )
}
