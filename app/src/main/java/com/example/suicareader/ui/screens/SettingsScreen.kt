package com.example.suicareader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.animation.core.animateDpAsState

@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val currentLang by themeViewModel.currentLanguage.collectAsState()
    
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    var langExpanded by remember { mutableStateOf(false) }
    
    val blurRadius by animateDpAsState(
        targetValue = if (langExpanded) 20.dp else 0.dp,
        label = "blur"
    )

    Column(modifier = Modifier.fillMaxSize().blur(blurRadius).padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { langExpanded = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.language, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(currentLang.name, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        if (langExpanded) {
            LanguageSelectionDialog(
                onDismiss = { langExpanded = false },
                onLanguageSelected = { lang ->
                    themeViewModel.setLanguage(lang)
                    langExpanded = false
                },
                currentLang = currentLang,
                textColor = textColor
            )
        }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GitHub Link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Schatten-Herz/SuicaReader"))
                    context.startActivity(intent)
                }
                .padding(16.dp)
        ) {
            Column {
                Text(strings.openSource, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(strings.openSourceDesc, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}
@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    currentLang: AppLanguage,
    textColor: Color
) {
    val strings = LocalStrings.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // 背景高斯模糊效果 (通过外部模糊很难，但在内容层做)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(40.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
            
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = strings.language,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LanguageOptionItem("English", AppLanguage.EN == currentLang, textColor) { onLanguageSelected(AppLanguage.EN) }
                Spacer(modifier = Modifier.height(12.dp))
                LanguageOptionItem("中文", AppLanguage.ZH == currentLang, textColor) { onLanguageSelected(AppLanguage.ZH) }
                Spacer(modifier = Modifier.height(12.dp))
                LanguageOptionItem("日本語", AppLanguage.JA == currentLang, textColor) { onLanguageSelected(AppLanguage.JA) }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.cancel, color = textColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun LanguageOptionItem(
    label: String,
    isSelected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textColor, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
