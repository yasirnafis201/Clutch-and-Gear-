package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDark
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onStartDemoMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                // Hero Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, CngGraphiteBorder, RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_cockpit_1791784551066),
                        contentDescription = "Cockpit",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, CngGraphiteDarkest.copy(alpha = 0.9f))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Clutch & Gear (C&G)",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = CngTextPrimary
                )
                Text(
                    text = "AI-Powered Vehicle Intelligence Platform",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CngCyanElectric
                )

                Spacer(modifier = Modifier.height(16.dp))

                OnboardingFeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Live OBD-II Telemetry",
                    description = "Stream engine RPM, speed, coolant temperature, manifold pressure, and fuel readings."
                )
                Spacer(modifier = Modifier.height(12.dp))
                OnboardingFeatureItem(
                    icon = Icons.Default.Shield,
                    title = "Deterministic Diagnostics",
                    description = "Instant DTC scan & physical threshold monitoring. 100% offline local rules."
                )
                Spacer(modifier = Modifier.height(12.dp))
                OnboardingFeatureItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Grounded AI Assistant",
                    description = "Ask questions with actual live telemetry context. Real answers with zero hallucinated faults."
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .testTag("onboarding_start_button")
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CngCyanElectric),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Connect Vehicle / Enter App",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngGraphiteDarkest
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onStartDemoMode,
                    modifier = Modifier
                        .testTag("onboarding_demo_button")
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Explore in Virtual Simulation Mode",
                        fontSize = 14.sp,
                        color = CngCyanElectric
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun OnboardingFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CngGraphiteDark)
            .border(1.dp, CngGraphiteBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CngGraphiteDarkest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CngCyanElectric,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = CngTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
