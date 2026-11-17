package com.example.suicareader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.suicareader.ui.components.GlassDialogPanel
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.GlassOverlayContainer
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.theme.AppLanguage
import com.example.suicareader.ui.theme.GlassBlurPreset
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalOverlayBackdrop
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.example.suicareader.ui.theme.ThemeViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel = viewModel()) {
    val context = LocalContext.current
    val currentLang by themeViewModel.currentLanguage.collectAsState()
    val betaEnabled by themeViewModel.betaEnabled.collectAsState()
    val blurPreset by themeViewModel.blurPreset.collectAsState()

    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    var langExpanded by remember { mutableStateOf(false) }
    var blurPresetExpanded by remember { mutableStateOf(false) }

    val appBackdrop = LocalAppBackdrop.current
    val pageBackdrop = rememberLayerBackdrop()
    val overlayBackdrop = if (appBackdrop != null) {
        rememberCombinedBackdrop(appBackdrop, pageBackdrop)
    } else {
        pageBackdrop
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 108.dp)
        ) {
            Text(
                text = strings.settingsTitle,
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
                    .clickable { langExpanded = true }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.language, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(currentLang.name, color = textColor.copy(alpha = 0.72f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.journeyBetaTitle, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(strings.journeyBetaDesc, color = textColor.copy(alpha = 0.72f), fontSize = 12.sp)
                    }
                    Switch(
                        checked = betaEnabled,
                        onCheckedChange = { themeViewModel.setBetaEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF22C55E).copy(alpha = 0.65f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
                    .clickable { blurPresetExpanded = true }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.glassBlurPreset, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (blurPreset == GlassBlurPreset.STRONG) strings.blurStrong else strings.blurWeak,
                            color = textColor.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Schatten-Herz/SuicaReader"))
                        context.startActivity(intent)
                    }
                    .padding(18.dp)
            ) {
                Column {
                    Text(strings.openSource, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(strings.openSourceDesc, color = textColor.copy(alpha = 0.72f), fontSize = 12.sp)
                }
            }
        }

        CompositionLocalProvider(LocalOverlayBackdrop provides appBackdrop) {
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

            if (blurPresetExpanded) {
                BlurPresetSelectionDialog(
                    onDismiss = { blurPresetExpanded = false },
                    onPresetSelected = { preset ->
                        themeViewModel.setBlurPreset(preset)
                        blurPresetExpanded = false
                    },
                    currentPreset = blurPreset,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
fun BlurPresetSelectionDialog(
    onDismiss: () -> Unit,
    onPresetSelected: (GlassBlurPreset) -> Unit,
    currentPreset: GlassBlurPreset,
    textColor: Color
) {
    val strings = LocalStrings.current

    GlassOverlayContainer(onDismissRequest = onDismiss) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = strings.glassBlurPreset,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LanguageOptionItem(strings.blurStrong, GlassBlurPreset.STRONG == currentPreset, textColor) {
                    onPresetSelected(GlassBlurPreset.STRONG)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LanguageOptionItem(strings.blurWeak, GlassBlurPreset.WEAK == currentPreset, textColor) {
                    onPresetSelected(GlassBlurPreset.WEAK)
                }

                Spacer(modifier = Modifier.height(22.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.cancel, color = textColor.copy(alpha = 0.75f))
                }
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

    GlassOverlayContainer(onDismissRequest = onDismiss) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
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

                Spacer(modifier = Modifier.height(22.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.cancel, color = textColor.copy(alpha = 0.75f))
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
            .glassSurface(
                level = GlassLevel.DialogItem,
                cornerRadius = 16.dp,
                isPressed = isSelected
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
