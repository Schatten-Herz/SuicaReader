package com.example.suicareader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val currentLang by themeViewModel.currentLanguage.collectAsState()
    
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = strings.settingsTitle,
            color = textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
        )

        // Dark Theme Switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.darkTheme, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(strings.darkThemeDesc, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { themeViewModel.toggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language
        var langExpanded by remember { mutableStateOf(false) }
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
            DropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("English") },
                    onClick = { themeViewModel.setLanguage(AppLanguage.EN); langExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("中文") },
                    onClick = { themeViewModel.setLanguage(AppLanguage.ZH); langExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("日本語") },
                    onClick = { themeViewModel.setLanguage(AppLanguage.JA); langExpanded = false }
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
