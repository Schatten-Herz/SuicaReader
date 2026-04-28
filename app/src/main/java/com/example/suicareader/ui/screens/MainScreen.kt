package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.suicareader.ui.theme.Motion
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.components.GlassLevel

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
    val betaEnabled by themeViewModel.betaEnabled.collectAsState()
    
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
                    journeyText = strings.journeyTitle,
                    settingsText = strings.settingsTitle,
                    showJourney = betaEnabled,
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
                if (betaEnabled) {
                    composable("journey") {
                        JourneyScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(betaEnabled, currentRoute) {
        if (!betaEnabled && currentRoute == "journey") {
            bottomNavController.navigate("dashboard") {
                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

@Composable
fun GlassBottomBar(
    currentRoute: String,
    textColor: Color,
    dashboardText: String,
    journeyText: String,
    settingsText: String,
    showJourney: Boolean,
    onNavigate: (String) -> Unit
) {
    val itemWidth = if (showJourney) 96.dp else 124.dp
    val itemSpacing = 6.dp
    val navItems = buildList {
        add(Triple("dashboard", dashboardText, Icons.Default.Home))
        if (showJourney) add(Triple("journey", journeyText, Icons.Default.Place))
        add(Triple("settings", settingsText, Icons.Default.Settings))
    }
    val selectedIndex = when (currentRoute) {
        "dashboard" -> 0
        "journey" -> if (showJourney) 1 else 0
        else -> navItems.lastIndex
    }
    val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = (itemWidth + itemSpacing) * selectedIndex,
        animationSpec = Motion.BottomBarSpring,
        label = "indicator"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(56.dp)
                .glassSurface(level = GlassLevel.Overlay, cornerRadius = 28.dp)
                .padding(4.dp)
        ) {
            // G2-like continuous ellipse indicator with compact vertical spacing.
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )

            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    BottomNavItem(
                        icon = item.third,
                        label = item.second,
                        selected = selectedIndex == index,
                        textColor = textColor,
                        itemWidth = itemWidth,
                        onClick = { onNavigate(item.first) }
                    )
                }
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
    itemWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val contentColor = if (selected) textColor else textColor.copy(alpha = 0.6f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) Motion.PressedScale else 1f,
        animationSpec = Motion.PressSpring,
        label = "bottom_item_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(itemWidth)
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label, 
            color = contentColor, 
            fontSize = 11.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
