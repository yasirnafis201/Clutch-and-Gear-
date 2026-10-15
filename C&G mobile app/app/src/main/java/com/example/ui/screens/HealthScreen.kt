package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.HealthBreakdown
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import com.example.ui.components.HealthScoreCard
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun HealthScreen(
    vehicle: VehicleProfile?,
    healthBreakdown: HealthBreakdown,
    recentSamples: List<VehicleSample>
) {
    val coolantHistory = recentSamples.filter { it.parameterId == "0105" }.map { it.value.toFloat() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Vehicle Intelligence Health Report",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = "Explainable diagnostics based on physical evidence",
                fontSize = 12.sp,
                color = CngTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            HealthScoreCard(breakdown = healthBreakdown)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Coolant Thermal Stability Trend Chart
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Coolant Thermal Stability Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = "Target: 85-98°C",
                            fontSize = 12.sp,
                            color = CngCyanElectric
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw reference normal band lines
                            val normY = h * 0.5f
                            drawLine(
                                color = CngGreenNominal.copy(alpha = 0.3f),
                                start = Offset(0f, normY),
                                end = Offset(w, normY),
                                strokeWidth = 2.dp.toPx()
                            )

                            if (coolantHistory.size >= 2) {
                                val minTemp = 70f
                                val maxTemp = 120f
                                val stepX = w / (coolantHistory.size - 1)

                                val path = Path()
                                coolantHistory.forEachIndexed { idx, temp ->
                                    val clamped = temp.coerceIn(minTemp, maxTemp)
                                    val norm = (clamped - minTemp) / (maxTemp - minTemp)
                                    val y = h - (norm * h)
                                    val x = idx * stepX
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = CngCyanElectric,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Historical sensor readings verify engine thermostat regulation without hot spikes.",
                        fontSize = 11.sp,
                        color = CngTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Component Wear Disclaimers
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Component Wear Estimates (Service Intervals)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OBD-II telemetry cannot directly measure brake pad thickness, tire tread depth, or timing belt fatigue. Wear estimations shown in C&G are calculated from logged maintenance mileage and verified sensor trends.",
                        fontSize = 12.sp,
                        color = CngTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
