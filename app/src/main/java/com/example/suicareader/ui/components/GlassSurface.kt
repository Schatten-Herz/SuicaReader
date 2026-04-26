package com.example.suicareader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassLevel {
    SurfacePrimary,
    SurfaceSecondary,
    Overlay
}

data class LiquidGlassStyle(
    val cornerRadius: Dp,
    val fillAlpha: Float,
    val borderAlphaStrong: Float,
    val borderAlphaWeak: Float,
    val depthShadowAlpha: Float,
    val glowAlpha: Float
)

object LiquidGlassTokens {
    fun style(level: GlassLevel): LiquidGlassStyle {
        return when (level) {
            GlassLevel.SurfacePrimary -> LiquidGlassStyle(
                cornerRadius = 24.dp,
                fillAlpha = 0.18f,
                borderAlphaStrong = 0.52f,
                borderAlphaWeak = 0.10f,
                depthShadowAlpha = 0.22f,
                glowAlpha = 0.12f
            )
            GlassLevel.SurfaceSecondary -> LiquidGlassStyle(
                cornerRadius = 20.dp,
                fillAlpha = 0.14f,
                borderAlphaStrong = 0.38f,
                borderAlphaWeak = 0.07f,
                depthShadowAlpha = 0.16f,
                glowAlpha = 0.08f
            )
            GlassLevel.Overlay -> LiquidGlassStyle(
                cornerRadius = 24.dp,
                fillAlpha = 0.24f,
                borderAlphaStrong = 0.58f,
                borderAlphaWeak = 0.14f,
                depthShadowAlpha = 0.28f,
                glowAlpha = 0.14f
            )
        }
    }
}

fun Modifier.glassSurface(
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.22f,
    borderAlphaStrong: Float = 0.55f,
    borderAlphaWeak: Float = 0.08f
): Modifier {
    val style = LiquidGlassStyle(
        cornerRadius = cornerRadius,
        fillAlpha = fillAlpha,
        borderAlphaStrong = borderAlphaStrong,
        borderAlphaWeak = borderAlphaWeak,
        depthShadowAlpha = 0.20f,
        glowAlpha = 0.1f
    )
    return this.glassSurface(style)
}

fun Modifier.glassSurface(
    level: GlassLevel,
    cornerRadius: Dp? = null
): Modifier {
    val base = LiquidGlassTokens.style(level)
    val style = if (cornerRadius == null) base else base.copy(cornerRadius = cornerRadius)
    return this.glassSurface(style)
}

fun Modifier.glassSurface(
    style: LiquidGlassStyle
): Modifier {
    val shape = RoundedCornerShape(style.cornerRadius)
    val shadowColor = Color.Black.copy(alpha = style.depthShadowAlpha)
    return this
        .shadow(elevation = 20.dp, shape = shape, ambientColor = shadowColor, spotColor = shadowColor)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = style.fillAlpha + 0.05f),
                    Color.White.copy(alpha = style.fillAlpha)
                )
            )
        )
        .background(
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = style.glowAlpha),
                    Color.Transparent
                ),
                center = Offset(120f, 0f),
                radius = 360f
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = style.borderAlphaStrong),
                    Color.White.copy(alpha = style.borderAlphaWeak)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}
