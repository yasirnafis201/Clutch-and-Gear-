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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.TripRecord
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
fun TripsScreen(
    trips: List<TripRecord>
) {
    val totalKm = trips.sumOf { it.distanceKm }
    val totalSamples = trips.sumOf { it.sampleCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Drive Session History",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = "Sessions logged from active OBD-II connections",
                fontSize = 12.sp,
                color = CngTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Summary statistics card
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
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f km", totalKm),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngCyanElectric
                        )
                        Text(text = "Total Distance", fontSize = 11.sp, color = CngTextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${trips.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngGreenNominal
                        )
                        Text(text = "Recorded Trips", fontSize = 11.sp, color = CngTextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalSamples",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(text = "Sensor Samples", fontSize = 11.sp, color = CngTextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(trips) { trip ->
            val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                .format(Date(trip.startTimeEpochMs))

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = String.format("%.1f km", trip.distanceKm),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngCyanElectric
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = CngTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Avg: ${trip.avgSpeedKmh.toInt()} km/h",
                                fontSize = 12.sp,
                                color = CngTextSecondary
                            )
                        }
                        Text(
                            text = "Peak: ${trip.maxRpm.toInt()} RPM",
                            fontSize = 12.sp,
                            color = CngTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
