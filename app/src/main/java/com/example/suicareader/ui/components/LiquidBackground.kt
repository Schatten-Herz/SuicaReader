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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val LIQUID_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform vec4 baseColor; // e.g. vec4(0.3, 0.69, 0.31, 1.0)
    
    vec4 main(float2 fragCoord) {
        vec2 uv = fragCoord / iResolution.xy;
        // Map to [-1, 1] for distortion
        vec2 p = uv * 2.0 - 1.0;
        
        float t = iTime * 0.4;
        
        // Liquid distortion logic
        vec2 q = vec2(
            p.x + sin(t + p.y * 3.0) * 0.4,
            p.y + cos(t + p.x * 3.0) * 0.4
        );
        
        float d = length(q);
        
        // Create an organic fluid gradient using baseColor
        // baseColor + varying brightness and soft hue shifts
        vec3 colorOffset = 0.5 * cos(iTime + uv.xyx + vec3(0, 2, 4));
        vec3 col = baseColor.rgb + colorOffset * 0.2;
        col *= exp(-d * 0.4);
        
        return vec4(col, 1.0);
    }
"""

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF4CAF50) // Default Suica Green
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(LIQUID_SHADER) }
        val transition = rememberInfiniteTransition(label = "time")
        val time by transition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time"
        )
        
        Canvas(modifier = modifier.fillMaxSize()) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            shader.setFloatUniform("baseColor", baseColor.red, baseColor.green, baseColor.blue, baseColor.alpha)
            drawRect(brush = ShaderBrush(shader))
        }
    } else {
        // Fallback for API < 33
        Box(modifier = modifier.fillMaxSize().background(baseColor))
    }
}
