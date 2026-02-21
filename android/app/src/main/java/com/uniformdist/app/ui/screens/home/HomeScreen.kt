package com.uniformdist.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onViewStats: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Uniform Distribution",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Take a photo of your outfit to log\nwhat you're wearing today.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Clothing illustration
            ClothingIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // View Statistics button with icon
            OutlinedButton(
                onClick = onViewStats,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Statistics")
            }
        }

            LargeFloatingActionButton(
                onClick = onTakePhoto,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ClothingIllustration(modifier: Modifier = Modifier) {
    val tealBg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
    val tealAccent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    val shirtColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    val shirtStroke = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
    val pantsColor = Color(0xFF4A5568).copy(alpha = 0.7f)
    val pantsStroke = Color(0xFF2D3748).copy(alpha = 0.8f)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Background blob shape
        drawOval(
            color = tealBg,
            topLeft = Offset(centerX - 160.dp.toPx(), centerY - 90.dp.toPx()),
            size = Size(320.dp.toPx(), 180.dp.toPx())
        )
        // Secondary accent blob
        drawCircle(
            color = tealAccent,
            radius = 70.dp.toPx(),
            center = Offset(centerX - 40.dp.toPx(), centerY + 10.dp.toPx())
        )

        // Draw T-Shirt (left side)
        drawTShirt(
            centerX = centerX - 50.dp.toPx(),
            centerY = centerY - 5.dp.toPx(),
            fillColor = shirtColor,
            strokeColor = shirtStroke
        )

        // Draw Pants (right side)
        drawPants(
            centerX = centerX + 55.dp.toPx(),
            centerY = centerY + 5.dp.toPx(),
            fillColor = pantsColor,
            strokeColor = pantsStroke
        )
    }
}

private fun DrawScope.drawTShirt(
    centerX: Float,
    centerY: Float,
    fillColor: Color,
    strokeColor: Color
) {
    val scale = 1.2f
    val path = Path().apply {
        // Neck left
        moveTo(centerX - 12.dp.toPx() * scale, centerY - 35.dp.toPx() * scale)
        // Left shoulder
        lineTo(centerX - 35.dp.toPx() * scale, centerY - 28.dp.toPx() * scale)
        // Left sleeve tip
        lineTo(centerX - 45.dp.toPx() * scale, centerY - 12.dp.toPx() * scale)
        // Left sleeve inner
        lineTo(centerX - 30.dp.toPx() * scale, centerY - 8.dp.toPx() * scale)
        // Left armpit
        lineTo(centerX - 25.dp.toPx() * scale, centerY - 16.dp.toPx() * scale)
        // Left body
        lineTo(centerX - 25.dp.toPx() * scale, centerY + 30.dp.toPx() * scale)
        // Bottom
        lineTo(centerX + 25.dp.toPx() * scale, centerY + 30.dp.toPx() * scale)
        // Right body
        lineTo(centerX + 25.dp.toPx() * scale, centerY - 16.dp.toPx() * scale)
        // Right sleeve inner
        lineTo(centerX + 30.dp.toPx() * scale, centerY - 8.dp.toPx() * scale)
        // Right sleeve tip
        lineTo(centerX + 45.dp.toPx() * scale, centerY - 12.dp.toPx() * scale)
        // Right shoulder
        lineTo(centerX + 35.dp.toPx() * scale, centerY - 28.dp.toPx() * scale)
        // Neck right
        lineTo(centerX + 12.dp.toPx() * scale, centerY - 35.dp.toPx() * scale)
        // Neck curve
        quadraticBezierTo(
            centerX, centerY - 28.dp.toPx() * scale,
            centerX - 12.dp.toPx() * scale, centerY - 35.dp.toPx() * scale
        )
        close()
    }

    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 2.dp.toPx()))
}

private fun DrawScope.drawPants(
    centerX: Float,
    centerY: Float,
    fillColor: Color,
    strokeColor: Color
) {
    val scale = 1.2f
    val path = Path().apply {
        // Waistband left
        moveTo(centerX - 22.dp.toPx() * scale, centerY - 35.dp.toPx() * scale)
        // Waistband right
        lineTo(centerX + 22.dp.toPx() * scale, centerY - 35.dp.toPx() * scale)
        // Right hip
        lineTo(centerX + 24.dp.toPx() * scale, centerY - 5.dp.toPx() * scale)
        // Right leg outer
        lineTo(centerX + 26.dp.toPx() * scale, centerY + 35.dp.toPx() * scale)
        // Right leg bottom
        lineTo(centerX + 5.dp.toPx() * scale, centerY + 35.dp.toPx() * scale)
        // Crotch right
        lineTo(centerX + 2.dp.toPx() * scale, centerY - 2.dp.toPx() * scale)
        // Crotch left
        lineTo(centerX - 2.dp.toPx() * scale, centerY - 2.dp.toPx() * scale)
        // Left leg bottom
        lineTo(centerX - 5.dp.toPx() * scale, centerY + 35.dp.toPx() * scale)
        // Left leg outer
        lineTo(centerX - 26.dp.toPx() * scale, centerY + 35.dp.toPx() * scale)
        // Left hip
        lineTo(centerX - 24.dp.toPx() * scale, centerY - 5.dp.toPx() * scale)
        close()
    }

    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 2.dp.toPx()))

    // Waistband line
    drawLine(
        color = strokeColor,
        start = Offset(centerX - 22.dp.toPx() * scale, centerY - 30.dp.toPx() * scale),
        end = Offset(centerX + 22.dp.toPx() * scale, centerY - 30.dp.toPx() * scale),
        strokeWidth = 1.5.dp.toPx()
    )
}
