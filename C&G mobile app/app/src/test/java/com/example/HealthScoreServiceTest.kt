package com.example

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.DtcType
import com.example.services.HealthScoreService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthScoreServiceTest {

    @Test
    fun testCleanVehicleScoresOptimal() {
        val breakdown = HealthScoreService.computeHealth(
            dtcs = emptyList(),
            recentSamples = emptyList(),
            maintenanceRecords = emptyList(),
            isLiveConnected = true
        )
        // With no DTCs, base DTC score is 40.
        assertEquals(40, breakdown.dtcScore)
        assertTrue(breakdown.overallScore >= 70)
    }

    @Test
    fun testHighSeverityDtcPenalizesScore() {
        val dtcHigh = DiagnosticCode(
            code = "P0115",
            category = "Powertrain",
            description = "ECT Circuit Malfunction",
            codeType = DtcType.STORED,
            severity = DtcSeverity.HIGH
        )

        val breakdown = HealthScoreService.computeHealth(
            dtcs = listOf(dtcHigh),
            recentSamples = emptyList(),
            maintenanceRecords = emptyList(),
            isLiveConnected = true
        )

        // 40 - 20 = 20
        assertEquals(20, breakdown.dtcScore)
    }
}
