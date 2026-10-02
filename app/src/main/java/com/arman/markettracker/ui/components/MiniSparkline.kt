package com.arman.markettracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arman.markettracker.data.model.PricePoint

/**
 * Tiny sparkline for list rows — no axes, no labels, just the shape.
 */
@Composable
fun MiniSparkline(
    points: List<PricePoint>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        if (points.size < 2) return@Canvas
        val min = points.minOf { it.price }
        val max = points.maxOf { it.price }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i.toFloat() / (points.size - 1) * size.width
            val y = (1f - ((p.price - min) / span).toFloat()) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx()))
        val last = points.last()
        val ly = (1f - ((last.price - min) / span).toFloat()) * size.height
        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = Offset(size.width - 2.dp.toPx(), ly)
        )
    }
}
