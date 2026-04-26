package com.example.suicareader.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp

object Motion {
    const val PressedScale = 0.972f

    val PressSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 560f
    )

    val LiquidEaseOut = CubicBezierEasing(0.18f, 0.82f, 0.22f, 1.0f)
    val LiquidEaseInOut = CubicBezierEasing(0.44f, 0.0f, 0.22f, 1.0f)

    val NavEnter = tween<IntOffset>(durationMillis = 420, easing = LiquidEaseOut)
    val NavExit = tween<IntOffset>(durationMillis = 300, easing = LiquidEaseInOut)
    val NavFadeIn = tween<Float>(durationMillis = 360, easing = LiquidEaseOut)
    val NavFadeOut = tween<Float>(durationMillis = 240, easing = LiquidEaseInOut)

    val BottomBarSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val RubberBandLight = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val RubberBandMedium = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )
    val RubberBandStrong = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}
