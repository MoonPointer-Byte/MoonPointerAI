package com.englishai.translator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoonPointerLogo(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val moon = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0.22f * s, 0.2f * s, 0.62f * s, 0.72f * s))
        }
        drawPath(moon, Color(0xFFE0E7FF))
        drawCircle(
            color = Color(0xFF0A0A12),
            radius = s * 0.17f,
            center = Offset(s * 0.52f, s * 0.38f)
        )
        drawLine(
            color = Color(0xFF67E8F9),
            start = Offset(s * 0.58f, s * 0.58f),
            end = Offset(s * 0.78f, s * 0.68f),
            strokeWidth = s * 0.045f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(Color(0xFF67E8F9), s * 0.055f, Offset(s * 0.8f, s * 0.7f))
        drawCircle(Color(0xFFF0FDFA), s * 0.02f, Offset(s * 0.8f, s * 0.7f))
    }
}
