package com.example.platform.obd

object PidRegistry {
    const val PID_SUPPORTED_01_20 = "0100"
    const val PID_SUPPORTED_21_40 = "0120"
    const val PID_SUPPORTED_41_60 = "0140"

    const val PID_ENGINE_LOAD = "0104"
    const val PID_COOLANT_TEMP = "0105"
    const val PID_MAP = "010B"
    const val PID_ENGINE_RPM = "010C"
    const val PID_VEHICLE_SPEED = "010D"
    const val PID_INTAKE_TEMP = "010F"
    const val PID_MAF = "0110"
    const val PID_THROTTLE_POS = "0111"
    const val PID_RUN_TIME = "011F"
    const val PID_FUEL_LEVEL = "012F"
    const val PID_MODULE_VOLTAGE = "0142"

    val ALL_DEFINITIONS: Map<String, PidDefinition> = listOf(
        PidDefinition(
            pid = PID_ENGINE_RPM,
            mode = "01",
            pidHex = "0C",
            name = "Engine RPM",
            shortName = "RPM",
            unit = "rpm",
            minValue = 0.0,
            maxValue = 8000.0,
            bytesExpected = 2,
            decode = { bytes ->
                if (bytes.size >= 2) {
                    val a = bytes[0]
                    val b = bytes[1]
                    ((a * 256) + b) / 4.0
                } else null
            }
        ),
        PidDefinition(
            pid = PID_VEHICLE_SPEED,
            mode = "01",
            pidHex = "0D",
            name = "Vehicle Speed",
            shortName = "Speed",
            unit = "km/h",
            minValue = 0.0,
            maxValue = 260.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) bytes[0].toDouble() else null
            }
        ),
        PidDefinition(
            pid = PID_COOLANT_TEMP,
            mode = "01",
            pidHex = "05",
            name = "Coolant Temperature",
            shortName = "Coolant",
            unit = "°C",
            minValue = -40.0,
            maxValue = 215.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) (bytes[0] - 40).toDouble() else null
            }
        ),
        PidDefinition(
            pid = PID_ENGINE_LOAD,
            mode = "01",
            pidHex = "04",
            name = "Calculated Engine Load",
            shortName = "Load",
            unit = "%",
            minValue = 0.0,
            maxValue = 100.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) (bytes[0] * 100.0) / 255.0 else null
            }
        ),
        PidDefinition(
            pid = PID_INTAKE_TEMP,
            mode = "01",
            pidHex = "0F",
            name = "Intake Air Temperature",
            shortName = "Intake",
            unit = "°C",
            minValue = -40.0,
            maxValue = 215.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) (bytes[0] - 40).toDouble() else null
            }
        ),
        PidDefinition(
            pid = PID_THROTTLE_POS,
            mode = "01",
            pidHex = "11",
            name = "Throttle Position",
            shortName = "Throttle",
            unit = "%",
            minValue = 0.0,
            maxValue = 100.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) (bytes[0] * 100.0) / 255.0 else null
            }
        ),
        PidDefinition(
            pid = PID_MAP,
            mode = "01",
            pidHex = "0B",
            name = "Intake Manifold Absolute Pressure",
            shortName = "MAP",
            unit = "kPa",
            minValue = 0.0,
            maxValue = 255.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) bytes[0].toDouble() else null
            }
        ),
        PidDefinition(
            pid = PID_MAF,
            mode = "01",
            pidHex = "10",
            name = "Mass Air Flow",
            shortName = "MAF",
            unit = "g/s",
            minValue = 0.0,
            maxValue = 655.35,
            bytesExpected = 2,
            decode = { bytes ->
                if (bytes.size >= 2) {
                    val a = bytes[0]
                    val b = bytes[1]
                    ((a * 256) + b) / 100.0
                } else null
            }
        ),
        PidDefinition(
            pid = PID_MODULE_VOLTAGE,
            mode = "01",
            pidHex = "42",
            name = "Control Module Voltage",
            shortName = "Voltage",
            unit = "V",
            minValue = 0.0,
            maxValue = 20.0,
            bytesExpected = 2,
            decode = { bytes ->
                if (bytes.size >= 2) {
                    val a = bytes[0]
                    val b = bytes[1]
                    ((a * 256) + b) / 1000.0
                } else null
            }
        ),
        PidDefinition(
            pid = PID_FUEL_LEVEL,
            mode = "01",
            pidHex = "2F",
            name = "Fuel Level Input",
            shortName = "Fuel",
            unit = "%",
            minValue = 0.0,
            maxValue = 100.0,
            bytesExpected = 1,
            decode = { bytes ->
                if (bytes.isNotEmpty()) (bytes[0] * 100.0) / 255.0 else null
            }
        ),
        PidDefinition(
            pid = PID_RUN_TIME,
            mode = "01",
            pidHex = "1F",
            name = "Engine Run Time",
            shortName = "Run Time",
            unit = "s",
            minValue = 0.0,
            maxValue = 65535.0,
            bytesExpected = 2,
            decode = { bytes ->
                if (bytes.size >= 2) {
                    val a = bytes[0]
                    val b = bytes[1]
                    ((a * 256) + b).toDouble()
                } else null
            }
        )
    ).associateBy { it.pid }

    /**
     * Parse 4 bytes of supported-PID bitmap into a set of supported PIDs in the given base range.
     * baseRange e.g. 0x00 for 01-20, 0x20 for 21-40, 0x40 for 41-60.
     */
    fun parseSupportedBitmap(bytes: List<Int>, baseRange: Int = 0): Set<String> {
        if (bytes.size < 4) return emptySet()
        val supported = mutableSetOf<String>()
        var bitIndex = 1
        for (b in bytes.take(4)) {
            for (i in 7 downTo 0) {
                val isSet = (b and (1 shl i)) != 0
                if (isSet) {
                    val pidNum = baseRange + bitIndex
                    val pidHex = String.format("%02X", pidNum)
                    supported.add("01$pidHex")
                }
                bitIndex++
            }
        }
        return supported
    }

    /**
     * Checks if the 32nd bit of the bitmap is set, indicating further PIDs exist (e.g. 0120 or 0140).
     */
    fun hasNextRange(bytes: List<Int>): Boolean {
        if (bytes.size < 4) return false
        val lastByte = bytes[3]
        return (lastByte and 0x01) != 0
    }
}
