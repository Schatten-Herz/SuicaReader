package com.example.suicareader.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language
import kotlin.math.PI

@Language("AGSL")
private const val LIQUID_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform vec4 baseColor;

    vec4 main(float2 fragCoord) {
        vec2 uv = fragCoord / max(iResolution.xy, vec2(1.0));
        float aspect = iResolution.x / max(iResolution.y, 1.0);
        vec2 p = (uv - 0.5) * vec2(aspect, 1.0) * 2.0;

        float t = iTime;

        // Slow, silky dark fluid domain warping
        vec2 warp = vec2(
            sin(p.y * 1.5 + t) * 0.34 + cos(p.x * 1.1 - t) * 0.22,
            cos(p.x * 1.4 - t) * 0.34 + sin(p.y * 1.2 + t) * 0.22
        );
        vec2 q = p + warp;

        // Soft, low-luminance ambient pools
        vec2 c1 = vec2(cos(t) * 0.48, sin(t) * 0.52 - 0.20);
        vec2 c2 = vec2(sin(t) * -0.52 + 0.22, cos(t) * 0.44 + 0.24);
        vec2 c3 = vec2(cos(t + 2.1) * 0.46 - 0.18, sin(t + 1.0) * -0.46);

        float orb1 = exp(-dot(q - c1, q - c1) * 2.2);
        float orb2 = exp(-dot(q - c2, q - c2) * 2.4);
        float orb3 = exp(-dot(q - c3, q - c3) * 2.1);

        // Two subtle flowing aurora light streams (keeps refraction visible without glare)
        float stream1 = exp(-abs(q.y * 0.85 + sin(q.x * 1.6 + t) * 0.42) * 4.2);
        float stream2 = exp(-abs(q.x * 0.90 + cos(q.y * 1.7 - t) * 0.40) * 4.8);

        vec3 bg = vec3(0.015, 0.022, 0.036); // Deep obsidian midnight base
        vec3 deepEmerald = mix(baseColor.rgb * 0.38, vec3(0.04, 0.34, 0.22), 0.65);
        vec3 deepCyan = vec3(0.03, 0.22, 0.36);
        vec3 deepIndigo = vec3(0.12, 0.10, 0.30);
        vec3 silkSheen = vec3(0.10, 0.38, 0.30);

        vec3 col = bg;
        col += deepEmerald * orb1 * 0.44;
        col += deepCyan * orb2 * 0.40;
        col += deepIndigo * orb3 * 0.34;
        col += silkSheen * stream1 * 0.22;
        col += deepCyan * stream2 * 0.18;

        // Soft vignette & global luminance cap so background never glares
        float vig = smoothstep(2.2, 0.25, length(p));
        col *= mix(0.55, 0.98, vig);
        col = col / (1.0 + col * 0.65);

        return vec4(clamp(col, 0.0, 1.0), 1.0);
    }
"""

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF15803D)
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(LIQUID_SHADER) }
        val transition = rememberInfiniteTransition(label = "liquid_bg_transition")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "liquid_phase"
        )

        Box(modifier = modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", phase)
                shader.setFloatUniform(
                    "baseColor",
                    baseColor.red,
                    baseColor.green,
                    baseColor.blue,
                    baseColor.alpha
                )
                drawRect(brush = ShaderBrush(shader))
            }
        }
    } else {
        // Dark ambient fallback for API < 33
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF05080F))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.22f),
                            Color(0xFF0C4A6E).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(240f, 360f),
                        radius = 950f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF312E81).copy(alpha = 0.20f),
                            Color(0xFF115E59).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(860f, 1520f),
                        radius = 1050f
                    )
                )
        )
    }
}
