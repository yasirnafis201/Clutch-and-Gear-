package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.MaintenanceRecord
import com.example.domain.models.VehicleProfile
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaintenanceScreen(
    vehicle: VehicleProfile?,
    records: List<MaintenanceRecord>,
    onAddRecord: (Double, String, String, String, Double, Double?, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Maintenance Logbook",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Text(
                        text = "Service history for ${vehicle?.displayName}",
                        fontSize = 12.sp,
                        color = CngTextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("log_service_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CngGreenNominal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CngGraphiteDarkest,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log Service",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngGraphiteDarkest
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(records) { record ->
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(record.serviceDateEpochMs))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.category,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = "$${record.cost.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngGreenNominal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dateStr • ${record.odometerKm.toInt()} km",
                        fontSize = 12.sp,
                        color = CngCyanElectric
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = record.workPerformed,
                        fontSize = 13.sp,
                        color = CngTextPrimary
                    )

                    if (record.partsReplaced.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Parts: ${record.partsReplaced}",
                            fontSize = 11.sp,
                            color = CngTextSecondary
                        )
                    }

                    if (record.nextDueKm != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = CngTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Next due at: ${record.nextDueKm.toInt()} km",
                                fontSize = 11.sp,
                                color = CngTextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showAddDialog) {
        LogServiceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { odo, cat, work, parts, cost, nextKm, notes ->
                onAddRecord(odo, cat, work, parts, cost, nextKm, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LogServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, Double, Double?, String) -> Unit
) {
    var category by remember { mutableStateOf("Engine Oil & Filter") }
    var odometerStr by remember { mutableStateOf("25000") }
    var workPerformed by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("80") }
    var nextDueKmStr by remember { mutableStateOf("30000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Maintenance Service", fontWeight = FontWeight.Bold, color = CngTextPrimary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Service Category") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CngTextPrimary,
                        unfocusedTextColor = CngTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it },
                        label = { Text("Odometer (km)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost ($)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = workPerformed,
                    onValueChange = { workPerformed = it },
                    label = { Text("Work Performed") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CngTextPrimary,
                        unfocusedTextColor = CngTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = parts,
                    onValueChange = { parts = it },
                    label = { Text("Parts Replaced") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CngTextPrimary,
                        unfocusedTextColor = CngTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nextDueKmStr,
                    onValueChange = { nextDueKmStr = it },
                    label = { Text("Next Due At (km)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CngTextPrimary,
                        unfocusedTextColor = CngTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val odo = odometerStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val nextKm = nextDueKmStr.toDoubleOrNull()
                    onConfirm(odo, category, workPerformed.ifBlank { category }, parts, cost, nextKm, "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = CngGreenNominal)
            ) {
                Text("Save Service Log", color = CngGraphiteDarkest, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CngTextSecondary)
            }
        },
        containerColor = CngGraphiteDark
    )
}
