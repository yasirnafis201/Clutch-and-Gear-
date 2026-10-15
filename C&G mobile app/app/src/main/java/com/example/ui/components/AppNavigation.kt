package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.VehicleProfile
import com.example.platform.obd.ConnectionState
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

enum class AppScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Speed),
    TELEMETRY("Gauges", Icons.Default.Tune),
    DIAGNOSTICS("Diagnostics", Icons.Default.Warning),
    AI_ASSISTANT("C&G AI", Icons.Default.AutoAwesome),
    GARAGE("Garage", Icons.Default.DirectionsCar)
}

@Composable
fun AppHeader(
    currentVehicle: VehicleProfile?,
    allVehicles: List<VehicleProfile>,
    connectionState: ConnectionState,
    onSelectVehicle: (String) -> Unit,
    onConnectClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showVehicleMenu by remember { mutableStateOf(false) }

    val (connColor, connText) = when (connectionState) {
        ConnectionState.CONNECTED -> Pair(CngGreenNominal, "Connected")
        ConnectionState.CONNECTING -> Pair(CngCyanElectric, "Connecting...")
        ConnectionState.STALE_DATA -> Pair(CngAmberWarning, "Stale Signal")
        ConnectionState.PERMISSION_DENIED -> Pair(CngRedAlert, "No Permission")
        ConnectionState.ERROR -> Pair(CngRedAlert, "OBD Error")
        else -> Pair(CngTextSecondary, "Disconnected")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CngGraphiteDark)
            .border(1.dp, CngGraphiteBorder)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Selector Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .testTag("vehicle_selector_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(CngGraphiteDarkest)
                        .clickable { showVehicleMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = CngCyanElectric,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentVehicle?.displayName ?: "Select Vehicle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngTextPrimary
                    )
                }

                DropdownMenu(
                    expanded = showVehicleMenu,
                    onDismissRequest = { showVehicleMenu = false },
                    modifier = Modifier.background(CngGraphiteDarkest)
                ) {
                    allVehicles.forEach { veh ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = veh.displayName,
                                    color = if (veh.id == currentVehicle?.id) CngCyanElectric else CngTextPrimary,
                                    fontWeight = if (veh.id == currentVehicle?.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSelectVehicle(veh.id)
                                showVehicleMenu = false
                            }
                        )
                    }
                }
            }

            // Connection Status Pill
            Row(
                modifier = Modifier
                    .testTag("obd_connection_pill")
                    .clip(RoundedCornerShape(20.dp))
                    .background(CngGraphiteDarkest)
                    .clickable { onConnectClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(connColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = connText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = connColor
                )
            }
        }
    }
}

@Composable
fun AppBottomNav(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CngGraphiteDark)
            .border(1.dp, CngGraphiteBorder)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppScreen.values().forEach { screen ->
                val isSelected = screen == currentScreen
                val tint = if (isSelected) CngCyanElectric else CngTextSecondary

                Column(
                    modifier = Modifier
                        .testTag("nav_${screen.name.lowercase()}")
                        .clickable { onScreenSelected(screen) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tint
                    )
                }
            }
        }
    }
}
