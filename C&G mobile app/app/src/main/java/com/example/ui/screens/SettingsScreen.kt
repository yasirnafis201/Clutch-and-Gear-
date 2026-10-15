package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.obd.DrivingProfile
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun SettingsScreen(
    currentProfile: DrivingProfile,
    onSelectDrivingProfile: (DrivingProfile) -> Unit
) {
    var isMetric by remember { mutableStateOf(true) }
    var voiceEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Application Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = "Vehicle parameters, testing, and sync",
                fontSize = 12.sp,
                color = CngTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Units preference
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Measurement Units",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = if (isMetric) "Metric (km/h, °C, kPa)" else "Imperial (mph, °F, psi)",
                            fontSize = 12.sp,
                            color = CngCyanElectric
                        )
                    }
                    Switch(
                        checked = isMetric,
                        onCheckedChange = { isMetric = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CngCyanElectric)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Voice preference
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Text-to-Speech Spoken Audio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = "Play voice audio for AI diagnostic answers",
                            fontSize = 12.sp,
                            color = CngTextSecondary
                        )
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { voiceEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CngCyanElectric)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Simulated Driving Scenario Selector
        item {
            Text(
                text = "Simulation Rig Driving Profiles",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val profiles = listOf(
                Pair(DrivingProfile.CITY_CRUISE, "City Cruising (Normal RPM & 88°C Coolant)"),
                Pair(DrivingProfile.HIGHWAY_SPRINT, "Highway Sprint (High Speed & 3200 RPM)"),
                Pair(DrivingProfile.IDLE, "Engine Idling (750 RPM, 0 km/h)"),
                Pair(DrivingProfile.OVERHEATING_FAULT, "Overheating Fault (Coolant climbs > 110°C, P0115)"),
                Pair(DrivingProfile.MISFIRE_FAULT, "Cylinder Misfire Fault (Unsteady RPM, P0300)")
            )

            profiles.forEach { (prof, desc) ->
                val isSel = prof == currentProfile
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) CngGraphiteDark else CngGraphiteDarkest)
                        .border(
                            1.dp,
                            if (isSel) CngCyanElectric else CngGraphiteBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectDrivingProfile(prof) }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = prof.name.replace("_", " "),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) CngCyanElectric else CngTextPrimary
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = CngTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Cloud Synchronization Status
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
                        text = "Supabase Cloud Sync Status",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Local SQLite persistence active. Offline queue synchronized upon network availability.",
                        fontSize = 12.sp,
                        color = CngTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // About & Legal Disclaimer
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
                        text = "Clutch & Gear (C&G) v1.0.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automotive Intelligence & Diagnostics Platform. C&G provides informational and maintenance-support assistance and is not a replacement for professional certified mechanical inspection.",
                        fontSize = 11.sp,
                        color = CngTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
