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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.domain.models.VehicleProfile
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGraphiteSurface
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextMuted
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun GarageScreen(
    vehicles: List<VehicleProfile>,
    selectedVehicleId: String?,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (String, String, String, Int, String, String, String, String, String) -> Unit,
    onDeleteVehicle: (String) -> Unit
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
                        text = "Vehicle Garage",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                    Text(
                        text = "Manage multi-vehicle profiles & telemetry",
                        fontSize = 12.sp,
                        color = CngTextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_vehicle_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CngCyanElectric),
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
                        text = "Add Car",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngGraphiteDarkest
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(vehicles) { veh ->
            val isSelected = veh.id == selectedVehicleId
            Box(
                modifier = Modifier
                    .testTag("vehicle_card_${veh.id}")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CngGraphiteDark)
                    .border(
                        1.dp,
                        if (isSelected) CngCyanElectric else CngGraphiteBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectVehicle(veh.id) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectVehicle(veh.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = CngCyanElectric)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = veh.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CngTextPrimary
                            )
                            Text(
                                text = "${veh.year} • ${veh.engineDetails.ifEmpty { veh.make }}",
                                fontSize = 12.sp,
                                color = CngCyanElectric
                            )
                            if (veh.vin.isNotBlank()) {
                                Text(
                                    text = "VIN: ${veh.vin}",
                                    fontSize = 11.sp,
                                    color = CngTextMuted
                                )
                            }
                        }
                    }

                    if (vehicles.size > 1) {
                        IconButton(onClick = { onDeleteVehicle(veh.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Vehicle",
                                tint = CngRedAlert.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, make, model, year, fuel, engine, trans, vin, notes ->
                onAddVehicle(name, make, model, year, fuel, engine, trans, vin, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("2022") }
    var engine by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Gasoline") }
    var transmission by remember { mutableStateOf("Automatic") }
    var vin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add New Vehicle", fontWeight = FontWeight.Bold, color = CngTextPrimary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Nickname (e.g. Daily Driver)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CngTextPrimary,
                        unfocusedTextColor = CngTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = make,
                        onValueChange = { make = it },
                        label = { Text("Make (e.g. Toyota)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model (e.g. GR86)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = engine,
                        onValueChange = { engine = it },
                        label = { Text("Engine (e.g. 2.4L Boxer)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CngTextPrimary,
                            unfocusedTextColor = CngTextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it },
                    label = { Text("VIN (Optional)") },
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
                    val y = yearStr.toIntOrNull() ?: 2022
                    val disp = name.ifBlank { "$yearStr $make $model" }
                    onConfirm(disp, make, model, y, fuelType, engine, transmission, vin, "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = CngCyanElectric)
            ) {
                Text("Save to Garage", color = CngGraphiteDarkest, fontWeight = FontWeight.Bold)
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
