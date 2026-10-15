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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.domain.models.AiChatMessage
import com.example.domain.models.VehicleProfile
import com.example.domain.models.VehicleSample
import com.example.services.GroundedAiResponse
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
fun AiAssistantScreen(
    vehicle: VehicleProfile?,
    messages: List<AiChatMessage>,
    isLoading: Boolean,
    isListening: Boolean,
    recentSamples: List<VehicleSample>,
    onSendMessage: (String) -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onSpeakText: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val coolant = recentSamples.firstOrNull { it.parameterId == "0105" }?.value?.toInt() ?: 88
    val rpm = recentSamples.firstOrNull { it.parameterId == "010C" }?.value?.toInt() ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CngGraphiteDarkest)
            .padding(16.dp)
    ) {
        // Grounding Context Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CngGraphiteDark)
                .border(1.dp, CngCyanElectric.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CngCyanElectric,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Grounded Vehicle Context Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CngTextPrimary
                        )
                        Text(
                            text = "${vehicle?.displayName} • Coolant ${coolant}°C • RPM $rpm",
                            fontSize = 11.sp,
                            color = CngCyanElectric
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CngGreenNominal.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Verified",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CngGreenNominal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Chips
        val suggestions = listOf(
            "How is my car doing?",
            "Is my coolant running hot?",
            "Explain my trouble codes",
            "What should I ask my mechanic?"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { sug ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CngGraphiteSurface)
                        .border(1.dp, CngGraphiteBorder, RoundedCornerShape(16.dp))
                        .clickable { onSendMessage(sug) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = sug,
                        fontSize = 12.sp,
                        color = CngTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Messages Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isUser) CngCyanElectric else CngGraphiteDark)
                            .border(1.dp, if (isUser) CngCyanElectric else CngGraphiteBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .fillMaxWidth(0.85f)
                    ) {
                        Column {
                            Text(
                                text = msg.content,
                                fontSize = 13.sp,
                                color = if (isUser) CngGraphiteDarkest else CngTextPrimary,
                                lineHeight = 18.sp
                            )

                            if (!isUser && msg.evidenceReferences.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Evidence: ${msg.evidenceReferences.joinToString("; ")}",
                                    fontSize = 10.sp,
                                    color = CngAmberWarning
                                )
                            }

                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak aloud",
                                        tint = CngCyanElectric,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onSpeakText(msg.content) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CngCyanElectric,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyzing vehicle telemetry & diagnostic rules...",
                            fontSize = 12.sp,
                            color = CngTextSecondary
                        )
                    }
                }
            }
        }

        // Voice Input Activity Bar
        if (isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CngRedAlert)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Listening... Speak your automotive question",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CngRedAlert
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field and Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask C&G about your vehicle...", fontSize = 13.sp) },
                modifier = Modifier
                    .testTag("ai_input_field")
                    .weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CngTextPrimary,
                    unfocusedTextColor = CngTextPrimary,
                    focusedBorderColor = CngCyanElectric,
                    unfocusedBorderColor = CngGraphiteBorder
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic Button
            IconButton(
                onClick = {
                    if (isListening) onStopVoiceInput() else onStartVoiceInput()
                },
                modifier = Modifier
                    .testTag("ai_voice_button")
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isListening) CngRedAlert else CngGraphiteDark)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) Color.White else CngCyanElectric,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val txt = inputText
                        inputText = ""
                        onSendMessage(txt)
                    }
                },
                modifier = Modifier
                    .testTag("ai_send_button")
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CngCyanElectric)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = CngGraphiteDarkest,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
