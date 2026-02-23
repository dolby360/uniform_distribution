package com.uniformdist.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private enum class DragHandle {
    NONE, MOVE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT
}

private const val HANDLE_RADIUS_PX = 60f
private const val MIN_SIZE_PX = 100f

@Composable
fun CropOverlay(
    cropRect: Rect,
    onCropRectChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnCropRectChanged by rememberUpdatedState(onCropRectChanged)
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = detectHandle(offset, currentCropRect)
                    },
                    onDragEnd = { activeHandle = DragHandle.NONE },
                    onDragCancel = { activeHandle = DragHandle.NONE },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newRect = applyDrag(
                            currentCropRect, activeHandle, dragAmount,
                            size.width.toFloat(), size.height.toFloat()
                        )
                        currentOnCropRectChanged(newRect)
                    }
                )
            }
    ) {
        // Semi-transparent overlay outside the crop area
        drawScrim(cropRect)

        // Dashed border around crop area
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        )

        // Corner handles
        val corners = listOf(
            cropRect.topLeft, cropRect.topRight,
            cropRect.bottomLeft, cropRect.bottomRight
        )
        for (corner in corners) {
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = corner
            )
            drawCircle(
                color = Color(0xFF6750A4),
                radius = 7.dp.toPx(),
                center = corner
            )
        }
    }
}

private fun DrawScope.drawScrim(cropRect: Rect) {
    val scrimColor = Color.Black.copy(alpha = 0.5f)
    val w = size.width
    val h = size.height

    // Top band
    drawRect(scrimColor, Offset.Zero, Size(w, cropRect.top))
    // Bottom band
    drawRect(scrimColor, Offset(0f, cropRect.bottom), Size(w, h - cropRect.bottom))
    // Left band (between top and bottom)
    drawRect(scrimColor, Offset(0f, cropRect.top), Size(cropRect.left, cropRect.height))
    // Right band (between top and bottom)
    drawRect(scrimColor, Offset(cropRect.right, cropRect.top), Size(w - cropRect.right, cropRect.height))
}

private fun detectHandle(point: Offset, rect: Rect): DragHandle {
    val r = HANDLE_RADIUS_PX

    // Check corners first (highest priority)
    if ((point - rect.topLeft).getDistance() < r) return DragHandle.TOP_LEFT
    if ((point - rect.topRight).getDistance() < r) return DragHandle.TOP_RIGHT
    if ((point - rect.bottomLeft).getDistance() < r) return DragHandle.BOTTOM_LEFT
    if ((point - rect.bottomRight).getDistance() < r) return DragHandle.BOTTOM_RIGHT

    // Check edges
    if (point.y in (rect.top - r)..(rect.top + r) && point.x in rect.left..rect.right) return DragHandle.TOP
    if (point.y in (rect.bottom - r)..(rect.bottom + r) && point.x in rect.left..rect.right) return DragHandle.BOTTOM
    if (point.x in (rect.left - r)..(rect.left + r) && point.y in rect.top..rect.bottom) return DragHandle.LEFT
    if (point.x in (rect.right - r)..(rect.right + r) && point.y in rect.top..rect.bottom) return DragHandle.RIGHT

    // Check inside rect (move)
    if (rect.contains(point)) return DragHandle.MOVE

    return DragHandle.NONE
}

private fun applyDrag(
    rect: Rect, handle: DragHandle, delta: Offset,
    maxWidth: Float, maxHeight: Float
): Rect {
    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom

    when (handle) {
        DragHandle.MOVE -> {
            val dx = delta.x.coerceIn(-left, maxWidth - right)
            val dy = delta.y.coerceIn(-top, maxHeight - bottom)
            left += dx; top += dy; right += dx; bottom += dy
        }
        DragHandle.TOP_LEFT -> { left += delta.x; top += delta.y }
        DragHandle.TOP_RIGHT -> { right += delta.x; top += delta.y }
        DragHandle.BOTTOM_LEFT -> { left += delta.x; bottom += delta.y }
        DragHandle.BOTTOM_RIGHT -> { right += delta.x; bottom += delta.y }
        DragHandle.TOP -> { top += delta.y }
        DragHandle.BOTTOM -> { bottom += delta.y }
        DragHandle.LEFT -> { left += delta.x }
        DragHandle.RIGHT -> { right += delta.x }
        DragHandle.NONE -> {}
    }

    // Enforce minimum size
    if (right - left < MIN_SIZE_PX) {
        if (handle.name.contains("LEFT")) left = right - MIN_SIZE_PX
        else right = left + MIN_SIZE_PX
    }
    if (bottom - top < MIN_SIZE_PX) {
        if (handle.name.contains("TOP")) top = bottom - MIN_SIZE_PX
        else bottom = top + MIN_SIZE_PX
    }

    // Clamp to canvas bounds
    left = max(0f, left)
    top = max(0f, top)
    right = min(maxWidth, right)
    bottom = min(maxHeight, bottom)

    return Rect(left, top, right, bottom)
}
