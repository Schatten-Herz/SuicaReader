package com.example.suicareader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassSurface(
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.22f,
    borderAlphaStrong: Float = 0.55f,
    borderAlphaWeak: Float = 0.08f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = fillAlpha))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlphaStrong),
                    Color.White.copy(alpha = borderAlphaWeak)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}
