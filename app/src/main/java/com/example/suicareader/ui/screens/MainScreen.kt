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
    
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    Box(modifier = Modifier.fillMaxSize()) {

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
    val selectedIndex = if (currentRoute == "dashboard") 0 else 1
    val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = (selectedIndex * (100 + 8)).dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "indicator"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(64.dp) // Fixed height for consistency
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(50)
                )
                .padding(6.dp) // Padding for the indicator to breathe
        ) {
            // Animated Indicator Pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f))
            )

            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = dashboardText,
                    selected = selectedIndex == 0,
                    textColor = textColor,
                    onClick = { onNavigate("dashboard") }
                )

                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = settingsText,
                    selected = selectedIndex == 1,
                    textColor = textColor,
                    onClick = { onNavigate("settings") }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val contentColor = if (selected) textColor else textColor.copy(alpha = 0.6f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        // 微调视觉重心，增加 2dp 顶部间距
        Spacer(modifier = Modifier.height(2.dp))
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = contentColor, 
            fontSize = 12.sp, 
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
