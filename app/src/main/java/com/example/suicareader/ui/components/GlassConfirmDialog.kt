package com.example.suicareader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.ui.theme.LocalStrings

@Composable
fun GlassConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    confirmColor: Color = Color.White,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    val strings = LocalStrings.current
    GlassOverlayContainer(onDismissRequest = onDismissRequest) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 460.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(strings.cancel, color = Color.White.copy(alpha = 0.78f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                            .clickable(onClick = onConfirmClick)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(confirmText, color = confirmColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
