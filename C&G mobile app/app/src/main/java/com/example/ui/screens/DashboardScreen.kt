package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.models.HealthBreakdown
import com.example.domain.models.VehicleAlert
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import com.example.platform.obd.ConnectionState
import com.example.ui.components.CircularGauge
import com.example.ui.components.HealthScoreCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGraphiteSurface
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun DashboardScreen(
    vehicle: VehicleProfile?,
    connectionState: ConnectionState,
    recentSamples: List<VehicleSample>,
    activeAlerts: List<VehicleAlert>,
    healthBreakdown: HealthBreakdown,
    onNavigateDiagnostics: () -> Unit,
    onNavigateTelemetry: () -> Unit,
    onNavigateAi: () -> Unit,
    onNavigateMaintenance: () -> Unit,
    onNavigateConnect: () -> Unit,
    onDismissAlert: (String) -> Unit
) {
    val rpm = recentSamples.firstOrNull { it.parameterId == "010C" }?.value?.toFloat() ?: 0f
    val speed = recentSamples.firstOrNull { it.parameterId == "010D" }?.value?.toFloat() ?: 0f
    val coolant = recentSamples.firstOrNull { it.parameterId == "0105" }?.value?.toFloat() ?: 88f
    val load = recentSamples.firstOrNull { it.parameterId == "0104" }?.value?.toFloat() ?: 24f
    val voltage = recentSamples.firstOrNull { it.parameterId == "0142" }?.value?.toFloat() ?: 14.1f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(horizontal = 16.dp)
    ) {
        // Hero Cockpit Art Banner
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit_1791784551066),
                    contentDescription = "Cockpit Telemetry",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, CngGraphiteDarkest.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = vehicle?.displayName ?: "2021 Mazda MX-5 Miata",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${vehicle?.engineDetails} • ${vehicle?.transmission}",
                        fontSize = 12.sp,
                        color = CngCyanElectric
                    )
                }
            }
        }

        // Active Alerts Section
        if (activeAlerts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Active Safety Alerts (${activeAlerts.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CngAmberWarning
                )
            }
            items(activeAlerts) { alert ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .testTag("alert_item_${alert.id}")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CngRedAlert.copy(alpha = 0.15f))
                        .border(1.dp, CngRedAlert, RoundedCornerShape(12.dp))
                        .clickable { onDismissAlert(alert.id) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CngRedAlert,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = alert.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CngTextPrimary
                                )
                                Text(
                                    text = alert.message,
                                    fontSize = 11.sp,
                                    color = CngTextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                        Text(
                            text = "Dismiss",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngRedAlert,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Twin Primary Gauges (RPM and Speed)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularGauge(
                        value = rpm,
                        minValue = 0f,
                        maxValue = 8000f,
                        title = "ENGINE",
                        unit = "RPM",
                        size = 145.dp,
                        warningThreshold = 6500f,
                        criticalThreshold = 7200f
                    )
                    CircularGauge(
                        value = speed,
                        minValue = 0f,
                        maxValue = 240f,
                        title = "SPEED",
                        unit = "KM/H",
                        size = 145.dp,
                        warningThreshold = 140f
                    )
                }
            }
        }

        // Metrics Grid Row
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Coolant",
                    value = String.format("%.0f", coolant),
                    unit = "°C",
                    icon = Icons.Default.Thermostat,
                    modifier = Modifier.weight(1f),
                    statusColor = if (coolant > 105f) CngRedAlert else CngGreenNominal
                )
                MetricCard(
                    title = "Engine Load",
                    value = String.format("%.0f", load),
                    unit = "%",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f),
                    statusColor = if (load > 85f) CngAmberWarning else CngGreenNominal
                )
                MetricCard(
                    title = "Voltage",
                    value = String.format("%.1f", voltage),
                    unit = "V",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f),
                    statusColor = if (voltage < 12.2f) CngAmberWarning else CngGreenNominal
                )
            }
        }

        // Quick Actions Grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Vehicle Intelligence Actions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Scan DTCs",
                    subtitle = "Check engine scan",
                    icon = Icons.Default.Warning,
                    color = CngAmberWarning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateDiagnostics
                )
                QuickActionCard(
                    title = "Ask C&G AI",
                    subtitle = "Grounded advice",
                    icon = Icons.Default.AutoAwesome,
                    color = CngCyanElectric,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateAi
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "All Gauges",
                    subtitle = "Live multi-stream",
                    icon = Icons.Default.Speed,
                    color = CngCyanElectric,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTelemetry
                )
                QuickActionCard(
                    title = "Maintenance",
                    subtitle = "Service logs & due",
                    icon = Icons.Default.Build,
                    color = CngGreenNominal,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateMaintenance
                )
            }
        }

        // Health Score Preview Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HealthScoreCard(breakdown = healthBreakdown)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CngGraphiteDark)
            .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = CngTextSecondary
            )
        }
    }
}
