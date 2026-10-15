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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.platform.obd.AdapterDevice
import com.example.platform.obd.AdapterTransport
import com.example.platform.obd.ConnectionState
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
fun ObdConnectScreen(
    currentTransport: AdapterTransport,
    connectionState: ConnectionState,
    activeDevice: AdapterDevice?,
    availableDevices: List<AdapterDevice>,
    onSelectTransport: (AdapterTransport) -> Unit,
    onDiscoverDevices: () -> Unit,
    onConnectDevice: (AdapterDevice) -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "OBD-II Hardware Link",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = "Switch between physical ELM327, C&G IoT, or simulator",
                fontSize = 12.sp,
                color = CngTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Transport Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CngGraphiteDark)
                    .border(1.dp, CngGraphiteBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TransportTab(
                    label = "ELM327 BT",
                    isSelected = currentTransport == AdapterTransport.BLUETOOTH_CLASSIC,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTransport(AdapterTransport.BLUETOOTH_CLASSIC) }
                )
                TransportTab(
                    label = "C&G IoT",
                    isSelected = currentTransport == AdapterTransport.CNG_IOT_CUSTOM,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTransport(AdapterTransport.CNG_IOT_CUSTOM) }
                )
                TransportTab(
                    label = "Simulator",
                    isSelected = currentTransport == AdapterTransport.SIMULATOR,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTransport(AdapterTransport.SIMULATOR) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Active Connection Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CngGraphiteDark)
                    .border(
                        1.dp,
                        if (isConnected) CngGreenNominal else CngGraphiteBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) CngGreenNominal else CngRedAlert)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) "Adapter Connected" else "Adapter Disconnected",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CngTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeDevice?.name ?: "No device linked",
                            fontSize = 12.sp,
                            color = CngCyanElectric
                        )
                    }

                    if (isConnected) {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = CngRedAlert)
                        ) {
                            Text("Disconnect", color = CngTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // iOS Hardware Advisory Notice
        item {
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CngAmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hardware Compatibility Notice",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Generic Bluetooth Classic adapters (SPP) are supported on Android. On iOS, generic Classic SPP adapters require Apple MFi authorization; iOS requires compatible BLE adapters or the upcoming C&G IoT adapter.",
                            fontSize = 11.sp,
                            color = CngTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Available / Paired Devices
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Devices (${availableDevices.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CngTextPrimary
                )

                OutlinedButton(onClick = onDiscoverDevices) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CngCyanElectric,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discover", fontSize = 12.sp, color = CngCyanElectric)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(availableDevices) { dev ->
            val isCurrent = dev.address == activeDevice?.address
            Box(
                modifier = Modifier
                    .testTag("device_item_${dev.address}")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CngGraphiteDark)
                    .border(
                        1.dp,
                        if (isCurrent) CngGreenNominal else CngGraphiteBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onConnectDevice(dev) }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isCurrent) CngGreenNominal else CngCyanElectric,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = dev.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CngTextPrimary
                            )
                            Text(
                                text = "${dev.address} • ${dev.transport.name}",
                                fontSize = 11.sp,
                                color = CngTextSecondary
                            )
                        }
                    }

                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CngGreenNominal,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Connect",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngCyanElectric
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TransportTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CngCyanElectric else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CngGraphiteDarkest else CngTextSecondary
        )
    }
}
