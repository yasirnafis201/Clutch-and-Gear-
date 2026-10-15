package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.HealthBreakdown
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGraphiteSurface
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun HealthScoreCard(
    breakdown: HealthBreakdown,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        breakdown.overallScore >= 80 -> CngGreenNominal
        breakdown.overallScore >= 60 -> CngAmberWarning
        else -> CngRedAlert
    }

    Box(
        modifier = modifier
            .testTag("health_score_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CngGraphiteSurface)
            .border(1.dp, CngGraphiteBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vehicle Health Index",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Text(
                        text = breakdown.statusLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = scoreColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CngGraphiteDarkest)
                        .border(1.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${breakdown.overallScore}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                        Text(
                            text = "/100",
                            fontSize = 14.sp,
                            color = CngTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Factor Bars
            HealthFactorRow("Diagnostic DTC Status", breakdown.dtcScore, 40, CngGreenNominal)
            Spacer(modifier = Modifier.height(8.dp))
            HealthFactorRow("Sensor Trend Stability", breakdown.trendScore, 30, CngCyanElectric)
            Spacer(modifier = Modifier.height(8.dp))
            HealthFactorRow("Maintenance Currency", breakdown.maintenanceScore, 20, CngAmberWarning)
            Spacer(modifier = Modifier.height(8.dp))
            HealthFactorRow("Sensor Data Coverage (${breakdown.dataCoveragePercent}%)", breakdown.dataCoveragePercent, 100, CngCyanElectric)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Note: ${breakdown.limitations}",
                fontSize = 11.sp,
                color = CngTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun HealthFactorRow(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    val fraction = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = CngTextSecondary
            )
            Text(
                text = "$score / $maxScore",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CngTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CngGraphiteDarkest
        )
    }
}
