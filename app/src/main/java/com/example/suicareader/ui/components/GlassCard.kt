package com.example.suicareader.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.suicareader.ui.theme.Motion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 果冻感缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Motion.PressedScale else 1f,
        animationSpec = Motion.PressSpring,
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassSurface(cornerRadius = 24.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // 去掉默认波纹，配合果冻缩放
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(24.dp)
    ) {
        content()
    }
}
