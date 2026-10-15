package com.example.services

import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.domain.models.HealthBreakdown
import com.example.domain.models.MaintenanceRecord
import com.example.domain.models.VehicleSample

object HealthScoreService {

    /**
     * Computes an explainable vehicle health score based on verifiable evidence.
     */
    fun computeHealth(
        dtcs: List<DiagnosticCode>,
        recentSamples: List<VehicleSample>,
        maintenanceRecords: List<MaintenanceRecord>,
        isLiveConnected: Boolean
    ): HealthBreakdown {
        // Factor 1: DTC Health (0 to 40 points)
        var dtcScore = 40
        for (code in dtcs) {
            when (code.severity) {
                DtcSeverity.CRITICAL -> dtcScore -= 30
                DtcSeverity.HIGH -> dtcScore -= 20
                DtcSeverity.MEDIUM -> dtcScore -= 10
                DtcSeverity.LOW -> dtcScore -= 5
                DtcSeverity.INFO -> dtcScore -= 0
            }
        }
        dtcScore = dtcScore.coerceIn(0, 40)

        // Factor 2: Sensor Trend Stability (0 to 30 points)
        var trendScore = 30
        val coolantSamples = recentSamples.filter { it.parameterId == "0105" }
        if (coolantSamples.isNotEmpty()) {
            val maxCoolant = coolantSamples.maxOf { it.value }
            if (maxCoolant > 110.0) {
                trendScore -= 20
            } else if (maxCoolant > 102.0) {
                trendScore -= 10
            }
        }

        val voltageSamples = recentSamples.filter { it.parameterId == "0142" }
        if (voltageSamples.isNotEmpty()) {
            val minVoltage = voltageSamples.minOf { it.value }
            if (minVoltage < 12.0) {
                trendScore -= 10
            }
        }
        trendScore = trendScore.coerceIn(0, 30)

        // Factor 3: Maintenance Currency (0 to 20 points)
        var maintScore = 20
        val now = System.currentTimeMillis()
        val overdueRecords = maintenanceRecords.filter { record ->
            record.nextDueDateEpochMs != null && record.nextDueDateEpochMs < now
        }
        if (overdueRecords.isNotEmpty()) {
            maintScore -= (overdueRecords.size * 8)
        }
        maintScore = maintScore.coerceIn(0, 20)

        // Factor 4: Telemetry Data Coverage (0 to 10 points)
        val uniquePids = recentSamples.map { it.parameterId }.distinct().size
        val dataCoveragePercent = if (isLiveConnected) {
            ((uniquePids.toDouble() / 10.0) * 100).toInt().coerceIn(10, 100)
        } else {
            30 // Baseline offline coverage from historical records
        }
        val coverageScore = ((dataCoveragePercent.toDouble() / 100.0) * 10).toInt().coerceIn(0, 10)

        val totalScore = (dtcScore + trendScore + maintScore + coverageScore).coerceIn(0, 100)

        val statusLabel = when {
            totalScore >= 90 -> "Optimal Condition"
            totalScore >= 75 -> "Good Operational Health"
            totalScore >= 55 -> "Attention Recommended"
            else -> "Urgent Inspection Advised"
        }

        val limitations = if (!isLiveConnected) {
            "Score calculated from saved vehicle history. Connect live OBD-II adapter for real-time sensor verification."
        } else if (dataCoveragePercent < 60) {
            "Some vehicle parameters (MAF/MAP/Fuel) are not reporting. Score reflects $dataCoveragePercent% sensor coverage."
        } else {
            "Verified from $uniquePids live sensor channels and latest ECU diagnostic trouble code scan."
        }

        return HealthBreakdown(
            overallScore = totalScore,
            dtcScore = dtcScore,
            trendScore = trendScore,
            maintenanceScore = maintScore,
            dataCoveragePercent = dataCoveragePercent,
            statusLabel = statusLabel,
            limitations = limitations
        )
    }
}
