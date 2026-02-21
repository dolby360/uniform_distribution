package com.uniformdist.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

@Composable
fun WearFrequencyChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary

    if (data.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No wear data for the last 30 days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val sortedEntries = data.entries.sortedBy { it.key }
    val maxValue = sortedEntries.maxOf { it.value }.coerceAtLeast(1)

    Card(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(16.dp)
        ) {
            val barWidth = size.width / sortedEntries.size.coerceAtLeast(1)
            val chartHeight = size.height

            sortedEntries.forEachIndexed { index, entry ->
                val barHeight = (entry.value.toFloat() / maxValue) * chartHeight
                val x = index * barWidth

                drawRect(
                    color = barColor,
                    topLeft = Offset(x + barWidth * 0.1f, chartHeight - barHeight),
                    size = Size(barWidth * 0.8f, barHeight)
                )

                // Draw count label
                drawContext.canvas.nativeCanvas.drawText(
                    entry.value.toString(),
                    x + barWidth / 2,
                    chartHeight - barHeight - 8f,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        color = android.graphics.Color.GRAY
                    }
                )
            }
        }
    }
}
