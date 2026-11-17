package com.example.suicareader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.ui.theme.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameCardDialog(
    initialName: String,
    initialNumber: String,
    onDismissRequest: () -> Unit,
    onSaveClick: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }
    val strings = LocalStrings.current

    GlassOverlayContainer(onDismissRequest = onDismissRequest) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = strings.editCard,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.cardNameLabel, color = Color.White.copy(alpha = 0.72f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = glassOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = number,
                    onValueChange = { if (it.length <= 4) number = it },
                    label = { Text(strings.cardNumberLabel, color = Color.White.copy(alpha = 0.72f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = glassOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(strings.cancel, color = Color.White.copy(alpha = 0.75f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                            .clickable { onSaveClick(name, number) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(strings.save, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
