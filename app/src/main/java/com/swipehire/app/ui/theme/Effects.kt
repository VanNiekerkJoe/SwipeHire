package com.swipehire.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min

/**
 * Soft layered color blobs behind hero content. Uses only DrawScope member
 * functions so it compiles on every Compose UI version.
 */
fun Modifier.auroraMesh(dark: Boolean): Modifier = this.drawBehind {
    val base = if (dark) SurfaceDark else SurfaceLight
    drawRect(color = base, topLeft = Offset.Zero, size = Size(size.width, size.height))
    val maxDim = max(size.width, size.height)

    // Three soft radial "spots" approximated with concentric translucent circles.
    drawSoftSpot(
        centerX = size.width * 0.18f,
        centerY = size.height * 0.10f,
        radius  = maxDim * 0.55f,
        color   = Violet40,
        alpha   = if (dark) 0.55f else 0.35f
    )
    drawSoftSpot(
        centerX = size.width * 0.92f,
        centerY = size.height * 0.05f,
        radius  = maxDim * 0.40f,
        color   = Sky,
        alpha   = if (dark) 0.32f else 0.20f
    )
    drawSoftSpot(
        centerX = size.width * 0.85f,
        centerY = size.height * 0.95f,
        radius  = maxDim * 0.50f,
        color   = Mint40,
        alpha   = if (dark) 0.30f else 0.18f
    )
}

/**
 * Soft radial glow behind an element.
 */
fun Modifier.glow(color: Color, radiusMultiplier: Float = 1.8f, alpha: Float = 0.55f): Modifier = this.drawBehind {
    val radius = max(size.width, size.height) * radiusMultiplier / 2f
    drawSoftSpot(
        centerX = center.x,
        centerY = center.y,
        radius  = radius,
        color   = color,
        alpha   = alpha
    )
}

/**
 * Faint concentric rings behind avatar badges.
 */
fun Modifier.decorativeRings(): Modifier = this.drawBehind {
    val ringColor = Color.White.copy(alpha = 0.10f)
    val c = Offset(size.width * 0.82f, size.height * 0.15f)
    val minDim = min(size.width, size.height)
    var r = minDim * 0.25f
    repeat(3) {
        drawCircle(
            color = ringColor,
            radius = r,
            center = c,
            style = Stroke(width = 1.5f)
        )
        r += minDim * 0.18f
    }
}

/**
 * Emulates a radial gradient by stacking a few translucent circles of the
 * same hue — cheap, works on every API level, and needs no Brush extension.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSoftSpot(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    alpha: Float
) {
    val layers = 6
    for (i in layers downTo 1) {
        val t = i / layers.toFloat()          // 1.0 -> outer, 0.16 -> inner
        drawCircle(
            color = color.copy(alpha = alpha * (1f - t) + alpha * 0.15f),
            radius = radius * t,
            center = Offset(centerX, centerY)
        )
    }
}