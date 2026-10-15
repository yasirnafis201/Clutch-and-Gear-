package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.VehicleSample
import com.example.platform.obd.ConnectionState
import com.example.ui.components.CircularGauge
import com.example.ui.components.MetricCard
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun TelemetryScreen(
    samples: List<VehicleSample>,
    connectionState: ConnectionState
) {
    var selectedFrequencyHz by remember { mutableStateOf(5) }

    val rpm = samples.firstOrNull { it.parameterId == "010C" }?.value?.toFloat() ?: 0f
    val speed = samples.firstOrNull { it.parameterId == "010D" }?.value?.toFloat() ?: 0f
    val coolant = samples.firstOrNull { it.parameterId == "0105" }?.value?.toFloat() ?: 88f
    val load = samples.firstOrNull { it.parameterId == "0104" }?.value?.toFloat() ?: 24f
    val throttle = samples.firstOrNull { it.parameterId == "0111" }?.value?.toFloat() ?: 18f
    val intake = samples.firstOrNull { it.parameterId == "010F" }?.value?.toFloat() ?: 29f
    val map = samples.firstOrNull { it.parameterId == "010B" }?.value?.toFloat() ?: 36f
    val voltage = samples.firstOrNull { it.parameterId == "0142" }?.value?.toFloat() ?: 14.2f
    val fuel = samples.firstOrNull { it.parameterId == "012F" }?.value?.toFloat() ?: 68f
    val runtime = samples.firstOrNull { it.parameterId == "011F" }?.value?.toFloat() ?: 1240f

    val isConnected = connectionState == ConnectionState.CONNECTED

    // Pulse animation for live data
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        // Controls Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) CngGreenNominal.copy(alpha = alpha) else CngRedAlert
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "LIVE TELEMETRY STREAM" else "OFFLINE STREAM",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) CngCyanElectric else CngTextSecondary
                )
            }

            // Frequency pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(20.dp))
                    .padding(2.dp)
            ) {
                listOf(1, 5, 10).forEach { hz ->
                    val isSel = selectedFrequencyHz == hz
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) CngCyanElectric else Color.Transparent)
                            .clickable { selectedFrequencyHz = hz }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${hz}Hz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) CngGraphiteDarkest else CngTextSecondary
                        )
                    }
                }
            }
        }

        if (connectionState == ConnectionState.STALE_DATA) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CngAmberWarning.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CngAmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Telemetry signal paused or adapter delayed. Displaying last verified frame.",
                        fontSize = 11.sp,
                        color = CngTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Gauge Highlight: Tachometer & Coolant
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularGauge(
                value = rpm,
                minValue = 0f,
                maxValue = 8000f,
                title = "TACHOMETER",
                unit = "RPM",
                size = 140.dp,
                warningThreshold = 6500f,
                criticalThreshold = 7200f
            )
            CircularGauge(
                value = coolant,
                minValue = 40f,
                maxValue = 130f,
                title = "COOLANT TEMP",
                unit = "°C",
                size = 140.dp,
                warningThreshold = 105f,
                criticalThreshold = 115f
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of Real-Time Sensor Metrics
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                MetricCard(
                    title = "Vehicle Speed",
                    value = String.format("%.0f", speed),
                    unit = "km/h",
                    icon = Icons.Default.Speed,
                    statusColor = CngCyanElectric
                )
            }
            item {
                MetricCard(
                    title = "Engine Load",
                    value = String.format("%.0f", load),
                    unit = "%",
                    icon = Icons.Default.Speed,
                    statusColor = if (load > 80f) CngAmberWarning else CngGreenNominal
                )
            }
            item {
                MetricCard(
                    title = "Throttle Position",
                    value = String.format("%.0f", throttle),
                    unit = "%",
                    icon = Icons.Default.Speed,
                    statusColor = CngCyanElectric
                )
            }
            item {
                MetricCard(
                    title = "Intake Air Temp",
                    value = String.format("%.0f", intake),
                    unit = "°C",
                    icon = Icons.Default.Thermostat,
                    statusColor = CngGreenNominal
                )
            }
            item {
                MetricCard(
                    title = "Manifold Pressure",
                    value = String.format("%.0f", map),
                    unit = "kPa",
                    icon = Icons.Default.Air,
                    statusColor = CngGreenNominal
                )
            }
            item {
                MetricCard(
                    title = "Module Voltage",
                    value = String.format("%.1f", voltage),
                    unit = "V",
                    icon = Icons.Default.ElectricBolt,
                    statusColor = if (voltage < 12.2f) CngAmberWarning else CngGreenNominal
                )
            }
            item {
                MetricCard(
                    title = "Fuel Level",
                    value = String.format("%.0f", fuel),
                    unit = "%",
                    icon = Icons.Default.LocalGasStation,
                    statusColor = if (fuel < 15f) CngAmberWarning else CngGreenNominal
                )
            }
            item {
                MetricCard(
                    title = "Engine Runtime",
                    value = String.format("%.0f", runtime),
                    unit = "sec",
                    icon = Icons.Default.Timer,
                    statusColor = CngGreenNominal
                )
            }
        }
    }
}
