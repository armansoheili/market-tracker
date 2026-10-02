package com.arman.markettracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arman.markettracker.ui.theme.Negative
import com.arman.markettracker.ui.theme.NegativeDark
import com.arman.markettracker.ui.theme.Positive
import com.arman.markettracker.ui.theme.PositiveDark
import com.arman.markettracker.util.toChangePct

/**
 * Subtle movement indicator: direction arrow + percentage.
 * Movement is never communicated by color alone (arrow + sign included).
 */
@Composable
fun ChangeBadge(
    changePct: Double,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val positive = changePct >= 0
    val color: Color = when {
        positive && darkTheme -> PositiveDark
        positive -> Positive
        darkTheme -> NegativeDark
        else -> Negative
    }
    val arrow = if (positive) "▲" else "▼"
    Surface(
        modifier = modifier.semantics {
            contentDescription = if (positive) "Up ${changePct.toChangePct()}" else "Down ${changePct.toChangePct()}"
        },
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = changePct.toChangePct(),
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}
