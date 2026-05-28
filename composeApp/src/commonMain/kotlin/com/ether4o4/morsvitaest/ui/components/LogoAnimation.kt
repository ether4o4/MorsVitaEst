package com.ether4o4.morsvitaest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.size(size)) {
        val diameter = this.size.minDimension
        val radius = diameter / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val ringStroke = (diameter * 0.035f).coerceAtLeast(2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFB91C1C),
                    Color(0xFF1A0000),
                    Color(0xFF050505),
                ),
                center = center,
                radius = radius,
            ),
            radius = radius * 0.84f,
            center = center,
        )
        drawCircle(
            color = Color(0xFFE5484D).copy(alpha = 0.55f),
            radius = radius * 0.96f,
            center = center,
            style = Stroke(width = ringStroke),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = ringStroke * 0.72f),
        )

        val textLayout = textMeasurer.measure(
            text = "MVE",
            style = TextStyle(
                color = Color(0xFFFFF2F2),
                fontSize = (diameter * 0.24f).sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
            ),
            constraints = Constraints(maxWidth = diameter.toInt()),
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                x = center.x - textLayout.size.width / 2f,
                y = center.y - textLayout.size.height / 2f,
            ),
        )

        val lineWidth = diameter * 0.44f
        drawLine(
            color = Color(0xFFE5484D),
            start = Offset(center.x - lineWidth / 2f, center.y + radius * 0.36f),
            end = Offset(center.x + lineWidth / 2f, center.y + radius * 0.36f),
            strokeWidth = ringStroke,
        )
    }
}
