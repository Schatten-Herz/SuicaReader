package com.example.suicareader.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.suicareader.ui.theme.GlassBlurPreset
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalBlurPreset
import com.example.suicareader.ui.theme.LocalDialogBackdrop
import com.example.suicareader.ui.theme.LocalOverlayBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

enum class GlassLevel {
    SurfacePrimary,
    SurfaceSecondary,
    Overlay,
    FloatingBar,
    ActiveTabIndicator,
    DialogItem
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
                fillAlpha = 0.07f,
                borderAlphaStrong = 0.36f,
                borderAlphaWeak = 0.10f,
                depthShadowAlpha = 0.22f,
                glowAlpha = 0.12f
            )
            GlassLevel.SurfaceSecondary -> LiquidGlassStyle(
                cornerRadius = 20.dp,
                fillAlpha = 0.05f,
                borderAlphaStrong = 0.28f,
                borderAlphaWeak = 0.08f,
                depthShadowAlpha = 0.14f,
                glowAlpha = 0.08f
            )
            GlassLevel.Overlay -> LiquidGlassStyle(
                cornerRadius = 26.dp,
                fillAlpha = 0.09f,
                borderAlphaStrong = 0.42f,
                borderAlphaWeak = 0.12f,
                depthShadowAlpha = 0.28f,
                glowAlpha = 0.14f
            )
            GlassLevel.FloatingBar -> LiquidGlassStyle(
                cornerRadius = 29.dp,
                fillAlpha = 0.05f,
                borderAlphaStrong = 0.20f,
                borderAlphaWeak = 0.06f,
                depthShadowAlpha = 0.26f,
                glowAlpha = 0.08f
            )
            GlassLevel.ActiveTabIndicator -> LiquidGlassStyle(
                cornerRadius = 24.dp,
                fillAlpha = 0.10f,
                borderAlphaStrong = 0.22f,
                borderAlphaWeak = 0.08f,
                depthShadowAlpha = 0.0f,
                glowAlpha = 0.10f
            )
            GlassLevel.DialogItem -> LiquidGlassStyle(
                cornerRadius = 16.dp,
                fillAlpha = 0.08f,
                borderAlphaStrong = 0.26f,
                borderAlphaWeak = 0.08f,
                depthShadowAlpha = 0.0f,
                glowAlpha = 0.10f
            )
        }
    }
}

private fun Modifier.fallbackGlassSurface(style: LiquidGlassStyle): Modifier {
    val shape = RoundedCornerShape(style.cornerRadius)
    val shadowColor = Color.Black.copy(alpha = style.depthShadowAlpha)
    val tintAlpha = (style.fillAlpha * 2.4f).coerceIn(0.14f, 0.28f)
    return this
        .then(
            if (style.depthShadowAlpha > 0f) {
                Modifier.shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = tintAlpha + 0.06f),
                    Color.White.copy(alpha = tintAlpha)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = style.borderAlphaStrong * 0.75f),
                    Color.White.copy(alpha = style.borderAlphaWeak)
                ),
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}

fun Modifier.glassSurface(
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.08f,
    borderAlphaStrong: Float = 0.62f,
    borderAlphaWeak: Float = 0.14f,
    backdropOverride: Backdrop? = null
): Modifier = composed {
    val style = LiquidGlassStyle(
        cornerRadius = cornerRadius,
        fillAlpha = fillAlpha,
        borderAlphaStrong = borderAlphaStrong,
        borderAlphaWeak = borderAlphaWeak,
        depthShadowAlpha = 0.22f,
        glowAlpha = 0.12f
    )
    this.glassSurface(
        style = style,
        levelForBlur = GlassLevel.SurfacePrimary,
        backdropOverride = backdropOverride
    )
}

fun Modifier.glassSurface(
    level: GlassLevel = GlassLevel.SurfacePrimary,
    cornerRadius: Dp? = null,
    backdropOverride: Backdrop? = null,
    exportedBackdrop: LayerBackdrop? = null,
    isPressed: Boolean = false
): Modifier = composed {
    val baseStyle = LiquidGlassTokens.style(level)
    val style = if (cornerRadius == null) baseStyle else baseStyle.copy(cornerRadius = cornerRadius)
    this.glassSurface(
        style = style,
        levelForBlur = level,
        backdropOverride = backdropOverride,
        exportedBackdrop = exportedBackdrop,
        isPressed = isPressed
    )
}

fun Modifier.glassSurface(
    style: LiquidGlassStyle,
    levelForBlur: GlassLevel = GlassLevel.SurfacePrimary,
    backdropOverride: Backdrop? = null,
    exportedBackdrop: LayerBackdrop? = null,
    isPressed: Boolean = false,
    tintColor: Color = Color.White
): Modifier = composed {
    val dialogBackdrop = LocalDialogBackdrop.current
    val overlayBackdrop = LocalOverlayBackdrop.current
    val appBackdrop = LocalAppBackdrop.current
    val backdrop = backdropOverride ?: when (levelForBlur) {
        GlassLevel.Overlay -> overlayBackdrop ?: appBackdrop
        GlassLevel.DialogItem -> dialogBackdrop ?: overlayBackdrop ?: appBackdrop
        else -> dialogBackdrop ?: appBackdrop
    }
    val blurPreset = LocalBlurPreset.current

    if (backdrop == null) {
        return@composed this.fallbackGlassSurface(style)
    }

    val blurRadiusDp = when (levelForBlur) {
        GlassLevel.ActiveTabIndicator -> 2.dp
        GlassLevel.DialogItem -> 4.dp
        GlassLevel.FloatingBar -> if (blurPreset == GlassBlurPreset.STRONG) 6.dp else 3.dp
        GlassLevel.SurfacePrimary -> if (blurPreset == GlassBlurPreset.STRONG) 16.dp else 8.dp
        GlassLevel.SurfaceSecondary -> if (blurPreset == GlassBlurPreset.STRONG) 12.dp else 6.dp
        GlassLevel.Overlay -> if (blurPreset == GlassBlurPreset.STRONG) 22.dp else 12.dp
    }

    val shapeProvider = { RoundedCornerShape(style.cornerRadius) }
    val renderEffectSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effectiveFillAlpha = if (renderEffectSupported) {
        style.fillAlpha + (if (isPressed) 0.04f else 0f)
    } else {
        (style.fillAlpha * 2.5f).coerceIn(0.16f, 0.30f)
    }

    this.drawBackdrop(
        backdrop = backdrop,
        shape = shapeProvider,
        effects = {
            val isOverlayOrDialog = levelForBlur == GlassLevel.Overlay || levelForBlur == GlassLevel.DialogItem
            colorControls(
                brightness = when {
                    isPressed -> 0.02f
                    isOverlayOrDialog -> -0.02f
                    else -> 0f
                },
                contrast = if (isOverlayOrDialog) 1.02f else 1.04f,
                saturation = if (isOverlayOrDialog) 1.10f else 1.22f
            )
            blur(blurRadiusDp.toPx())
            when (levelForBlur) {
                GlassLevel.ActiveTabIndicator -> {
                    val minDim = size.minDimension
                    if (minDim > 0f) {
                        val height = (minDim * 0.28f).coerceAtMost(14.dp.toPx())
                        val amount = (minDim * 0.55f).coerceAtMost(28.dp.toPx())
                        lens(
                            refractionHeight = height,
                            refractionAmount = amount,
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    }
                }
                GlassLevel.FloatingBar -> {
                    val height = 14.dp.toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = 32.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                }
                GlassLevel.DialogItem -> {
                    val height = 8.dp.toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = 18.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = false
                    )
                }
                GlassLevel.Overlay -> {
                    val height = (if (blurPreset == GlassBlurPreset.STRONG) 16.dp else 12.dp).toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = height * 2.1f,
                        depthEffect = true,
                        chromaticAberration = true
                    )
                }
                GlassLevel.SurfacePrimary -> {
                    val height = (if (blurPreset == GlassBlurPreset.STRONG) 14.dp else 10.dp).toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = height * 2.0f,
                        depthEffect = true,
                        chromaticAberration = true
                    )
                }
                GlassLevel.SurfaceSecondary -> {
                    val height = (if (blurPreset == GlassBlurPreset.STRONG) 10.dp else 7.dp).toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = height * 1.8f,
                        depthEffect = true,
                        chromaticAberration = blurPreset == GlassBlurPreset.STRONG
                    )
                }
            }
        },
        highlight = {
            val isSubtleRim = levelForBlur == GlassLevel.FloatingBar ||
                levelForBlur == GlassLevel.ActiveTabIndicator ||
                levelForBlur == GlassLevel.DialogItem
            Highlight(
                width = if (isSubtleRim) 0.4.dp else 0.5.dp,
                blurRadius = if (isSubtleRim) 0.35.dp else 0.45.dp,
                alpha = when {
                    isPressed -> 0.85f
                    isSubtleRim -> 0.55f
                    else -> 0.75f
                },
                style = HighlightStyle.Default(
                    color = Color.White.copy(alpha = style.borderAlphaStrong),
                    angle = 45f,
                    falloff = if (isSubtleRim) 1.85f else 1.4f
                )
            )
        },
        shadow = if (style.depthShadowAlpha > 0f) {
            {
                Shadow(
                    radius = when (levelForBlur) {
                        GlassLevel.Overlay, GlassLevel.FloatingBar -> 24.dp
                        GlassLevel.SurfacePrimary -> 22.dp
                        else -> 14.dp
                    },
                    offset = DpOffset(0.dp, 6.dp),
                    color = Color.Black.copy(alpha = style.depthShadowAlpha)
                )
            }
        } else {
            null
        },
        innerShadow = when (levelForBlur) {
            GlassLevel.ActiveTabIndicator -> {
                {
                    InnerShadow(
                        radius = 6.dp,
                        offset = DpOffset(0.dp, 1.dp),
                        color = Color.White.copy(alpha = 0.10f)
                    )
                }
            }
            GlassLevel.SurfacePrimary, GlassLevel.Overlay -> {
                {
                    InnerShadow(
                        radius = 12.dp,
                        offset = DpOffset(0.dp, 2.dp),
                        color = Color.White.copy(alpha = if (isPressed) 0.18f else 0.08f)
                    )
                }
            }
            else -> null
        },
        exportedBackdrop = exportedBackdrop,
        onDrawSurface = {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = effectiveFillAlpha + 0.03f),
                        tintColor.copy(alpha = (effectiveFillAlpha * 0.6f).coerceAtLeast(0.02f))
                    )
                )
            )
        }
    )
}

val LocalOverlayCounter = staticCompositionLocalOf<MutableIntState?> { null }

/**
 * In-window modal overlay host so glass dialogs share the Activity Window
 * and can sample/refract the underlying UI through [LayerBackdrop].
 */
@Composable
fun GlassOverlayContainer(
    onDismissRequest: () -> Unit,
    scrimAlpha: Float = 0.34f,
    content: @Composable BoxScope.() -> Unit
) {
    val overlayCounter = LocalOverlayCounter.current
    DisposableEffect(overlayCounter) {
        overlayCounter?.let { it.intValue += 1 }
        onDispose {
            overlayCounter?.let { it.intValue = (it.intValue - 1).coerceAtLeast(0) }
        }
    }
    BackHandler(onBack = onDismissRequest)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Liquid glass modal panel that exports its own surface backdrop so nested buttons
 * and list items inside the dialog refract the dialog's underlying content cleanly
 * instead of falling back to the dark root background.
 */
@Composable
fun GlassDialogPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val dialogSurfaceBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier
            .glassSurface(
                level = GlassLevel.Overlay,
                cornerRadius = cornerRadius,
                exportedBackdrop = dialogSurfaceBackdrop
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        CompositionLocalProvider(LocalDialogBackdrop provides dialogSurfaceBackdrop) {
            content()
        }
    }
}
