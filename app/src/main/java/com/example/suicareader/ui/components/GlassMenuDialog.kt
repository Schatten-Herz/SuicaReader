package com.example.suicareader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassMenuDialog(
    onDismissRequest: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    title: String = "Card Options",
    editText: String = "Edit Card",
    deleteText: String = "Delete Card"
) {
    GlassOverlayContainer(onDismissRequest = onDismissRequest) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 440.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 6.dp)
                )

                GlassMenuItem(
                    icon = Icons.Default.Edit,
                    text = editText,
                    textColor = Color.White.copy(alpha = 0.94f),
                    iconColor = Color.White.copy(alpha = 0.94f),
                    onClick = onEditClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassMenuItem(
                    icon = Icons.Default.Delete,
                    text = deleteText,
                    textColor = Color(0xFFFF6B6B),
                    iconColor = Color(0xFFFF6B6B),
                    onClick = {
                        onDeleteClick()
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

@Composable
fun GlassMenuItem(
    icon: ImageVector,
    text: String,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
