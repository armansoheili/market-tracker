package com.arman.markettracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arman.markettracker.data.model.PricePoint

/**
 * Minimalist line chart for the detail screen.
 *
 * Subtle by design: faint grid, thin line, soft fill, one end dot.
 * Transitions smoothly (draw-on animation) when the data or range changes.
 */
@Composable
fun PriceChart(
    points: List<PricePoint>,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    // Re-run the draw-on animation whenever the dataset identity changes.
    val progress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        progress.animateTo(1f, animationSpec = tween(600))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (points.size < 2) return@Canvas

        val min = points.minOf { it.price }
        val max = points.maxOf { it.price }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val padTop = 12.dp.toPx()
        val padBottom = 12.dp.toPx()
        val h = size.height - padTop - padBottom

        fun x(i: Int) = i.toFloat() / (points.size - 1) * size.width
        fun y(p: Double) = padTop + (1f - ((p - min) / span).toFloat()) * h

        // Faint horizontal grid.
        repeat(4) { g ->
            val gy = padTop + h * g / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Smoothed line (quadratic through midpoints).
        val line = Path()
        val n = points.size
        val visible = (n * progress.value).toInt().coerceAtLeast(2)
        var prevX = x(0); var prevY = y(points[0].price)
        line.moveTo(prevX, prevY)
        for (i in 1 until visible) {
            val cx = x(i); val cy = y(points[i].price)
            line.quadraticTo(prevX, prevY, (prevX + cx) / 2, (prevY + cy) / 2)
            prevX = cx; prevY = cy
        }
        if (visible < n) {
            // Hold the last drawn point steady while animating.
            line.lineTo(prevX, prevY)
        }

        // Soft area fill under the line.
        val fill = Path().apply {
            addPath(line)
            lineTo(prevX, size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0f))
            )
        )
        drawPath(line, color = accent, style = Stroke(width = 2.dp.toPx()))

        // End dot at the latest drawn point.
        drawCircle(accent, radius = 3.5.dp.toPx(), center = Offset(prevX, prevY))
        drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(prevX, prevY))
    }
}
