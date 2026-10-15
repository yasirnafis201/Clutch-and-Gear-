package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.models.DiagnosticScanResult
import com.example.domain.models.VehicleProfile
import com.example.ui.components.DtcCard
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
fun DiagnosticsScreen(
    vehicle: VehicleProfile?,
    latestScan: DiagnosticScanResult?,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onClearCodes: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    val codes = latestScan?.codes ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        // Hero Scan Status Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CngGraphiteDark)
                    .border(
                        1.dp,
                        if (codes.isNotEmpty()) CngAmberWarning else CngGraphiteBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OBD-II Diagnostic Scanner",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CngTextPrimary
                            )
                            Text(
                                text = if (latestScan == null) "Ready for ECU scan" else "Scanned for ${vehicle?.displayName}",
                                fontSize = 12.sp,
                                color = CngTextSecondary
                            )
                        }

                        Icon(
                            imageVector = if (codes.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (codes.isEmpty()) CngGreenNominal else CngAmberWarning,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isScanning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = CngCyanElectric,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Querying ECU Mode 03 stored & pending trouble codes...",
                                fontSize = 13.sp,
                                color = CngCyanElectric
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            modifier = Modifier
                                .testTag("start_scan_button")
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CngCyanElectric),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CngGraphiteDarkest,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (latestScan == null) "Scan Vehicle Now" else "Scan Again",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CngGraphiteDarkest
                            )
                        }
                    }
                }
            }
        }

        // Read-Only Automotive Safety Notice
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CngCyanElectric,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Safety Guardrail: Read-Only Diagnostics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "C&G strictly issues passive Mode 01/03 queries. Arbitrary ECU commands or parameter overwrites are prevented to protect your powertrain safety.",
                            fontSize = 11.sp,
                            color = CngTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // DTC Results Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported Trouble Codes (${codes.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CngTextPrimary
                )

                if (codes.isNotEmpty()) {
                    TextButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = CngRedAlert,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear Codes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngRedAlert
                        )
                    }
                }
            }
        }

        if (codes.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CngGraphiteDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CngGreenNominal,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Trouble Codes Stored",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = "Powertrain and emissions systems are reporting nominal operation.",
                            fontSize = 12.sp,
                            color = CngTextSecondary
                        )
                    }
                }
            }
        } else {
            items(codes) { dtc ->
                Spacer(modifier = Modifier.height(10.dp))
                DtcCard(dtc = dtc)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Two-step Safety Clear Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = {
                Text(
                    text = "Clear Trouble Codes?",
                    fontWeight = FontWeight.Bold,
                    color = CngRedAlert
                )
            },
            text = {
                Text(
                    text = "Clearing Diagnostic Trouble Codes (Mode 04) resets check engine illumination and clears freeze-frame sensor logs. It DOES NOT mechanically repair underlying physical issues. Mechanics require freeze-frame data to diagnose problems.",
                    fontSize = 13.sp,
                    color = CngTextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        onClearCodes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CngRedAlert)
                ) {
                    Text("I Understand, Clear Codes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = CngTextSecondary)
                }
            },
            containerColor = CngGraphiteDark,
            titleContentColor = CngRedAlert,
            textContentColor = CngTextPrimary
        )
    }
}
