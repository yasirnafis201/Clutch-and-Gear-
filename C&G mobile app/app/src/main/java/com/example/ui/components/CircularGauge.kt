package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CngAmberWarning
import com.example.ui.theme.CngCyanElectric
import com.example.ui.theme.CngGraphiteBorder
import com.example.ui.theme.CngRedAlert
import com.example.ui.theme.CngTextPrimary
import com.example.ui.theme.CngTextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    warningThreshold: Float? = null,
    criticalThreshold: Float? = null
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(minValue, maxValue),
        animationSpec = tween(durationMillis = 200),
        label = "gauge_val"
    )

    val fraction = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val startAngle = 140f
    val sweepAngle = 260f

    val gaugeColor = when {
        criticalThreshold != null && animatedValue >= criticalThreshold -> CngRedAlert
        warningThreshold != null && animatedValue >= warningThreshold -> CngAmberWarning
        else -> CngCyanElectric
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Track
            drawArc(
                color = CngGraphiteBorder,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Arc
            val currentSweep = sweepAngle * fraction
            if (currentSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(CngCyanElectric, gaugeColor)
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Needle indicator dot
            val angleRad = Math.toRadians((startAngle + currentSweep).toDouble())
            val radius = (this.size.width - strokeWidth) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val needleX = (center.x + radius * cos(angleRad)).toFloat()
            val needleY = (center.y + radius * sin(angleRad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(needleX, needleY)
            )
        }

        // Center Readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (value >= 1000) String.format("%.0f", animatedValue) else String.format("%.1f", animatedValue),
                fontSize = if (size > 140.dp) 26.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = CngTextPrimary
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = gaugeColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = CngTextSecondary
            )
        }
    }
}
