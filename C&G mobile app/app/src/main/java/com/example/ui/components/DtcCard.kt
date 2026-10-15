package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
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
import com.example.domain.models.DiagnosticCode
import com.example.domain.models.DtcSeverity
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngGraphiteDarkest
import com.example.ui.theme.CngGraphiteSurface
import com.example.ui.theme.CngGreenNominal
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextMuted
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary

@Composable
fun DtcCard(
    dtc: DiagnosticCode,
    modifier: Modifier = Modifier,
    onDetailsClick: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val (badgeBg, badgeText) = when (dtc.severity) {
        DtcSeverity.CRITICAL -> Pair(CngRedAlert, Color.White)
        DtcSeverity.HIGH -> Pair(CngRedAlert.copy(alpha = 0.2f), CngRedAlert)
        DtcSeverity.MEDIUM -> Pair(CngAmberWarning.copy(alpha = 0.2f), CngAmberWarning)
        else -> Pair(CngGreenNominal.copy(alpha = 0.2f), CngGreenNominal)
    }

    Box(
        modifier = modifier
            .testTag("dtc_card_${dtc.code}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CngGraphiteSurface)
            .border(1.dp, CngGraphiteBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CngGraphiteDarkest)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = dtc.code,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dtc.category,
                        fontSize = 12.sp,
                        color = CngTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dtc.severity.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dtc.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CngTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) "Hide Diagnostic Advice" else "Tap for Diagnostic Guidance",
                    fontSize = 12.sp,
                    color = CngAmberWarning
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CngAmberWarning,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (dtc.possibleCauses.isNotEmpty()) {
                        Text(
                            text = "Possible Causes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextSecondary
                        )
                        dtc.possibleCauses.forEach { cause ->
                            Text(
                                text = "• $cause",
                                fontSize = 12.sp,
                                color = CngTextPrimary,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (dtc.recommendedActions.isNotEmpty()) {
                        Text(
                            text = "Recommended Next Steps:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextSecondary
                        )
                        dtc.recommendedActions.forEach { act ->
                            Text(
                                text = "→ $act",
                                fontSize = 12.sp,
                                color = CngGreenNominal,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
