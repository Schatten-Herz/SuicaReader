package com.example.suicareader.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp

object Motion {
    const val PressedScale = 0.96f

    val PressSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

    val NavEnter = tween<IntOffset>(durationMillis = 320)
    val NavExit = tween<IntOffset>(durationMillis = 220)
    val NavFadeIn = tween<Float>(durationMillis = 260)
    val NavFadeOut = tween<Float>(durationMillis = 200)

    val BottomBarSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
