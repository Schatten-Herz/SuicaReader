package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.LiquidBackground
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

import androidx.compose.runtime.collectAsState
import com.example.suicareader.ui.theme.ThemeViewModel

import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    themeViewModel: ThemeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val baseColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground(baseColor = baseColor)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    textColor = textColor,
                    dashboardText = strings.dashboardTitle,
                    settingsText = strings.settingsTitle,
                    onNavigate = { route ->
                        bottomNavController.navigate(route) {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = bottomNavController,
                startDestination = "dashboard",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("dashboard") {
                    DashboardScreen(viewModel, sharedTransitionScope, animatedVisibilityScope, onCardClick)
                }
                composable("settings") {
                    SettingsScreen(themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun GlassBottomBar(
    currentRoute: String,
    textColor: Color,
    dashboardText: String,
    settingsText: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
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
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dashboardSelected = currentRoute == "dashboard"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onNavigate("dashboard") }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = dashboardText,
                    tint = if (dashboardSelected) textColor else textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
                Text(dashboardText, color = if (dashboardSelected) textColor else textColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }

            val settingsSelected = currentRoute == "settings"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onNavigate("settings") }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = settingsText,
                    tint = if (settingsSelected) textColor else textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
                Text(settingsText, color = if (settingsSelected) textColor else textColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}
