package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.LocalOverlayCounter
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.example.suicareader.ui.theme.Motion
import com.example.suicareader.ui.theme.ThemeViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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

    val appBackdrop = LocalAppBackdrop.current
    val pageContentBackdrop = rememberLayerBackdrop()
    val barBackdrop: Backdrop = if (appBackdrop != null) {
        rememberCombinedBackdrop(appBackdrop, pageContentBackdrop)
    } else {
        pageContentBackdrop
    }
    val overlayCount = remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalOverlayCounter provides overlayCount) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content fills full screen and flows behind the floating GlassBottomBar
            NavHost(
                navController = bottomNavController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageContentBackdrop)
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

            AnimatedVisibility(
                visible = overlayCount.intValue == 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                GlassBottomBar(
                    currentRoute = currentRoute,
                    textColor = textColor,
                    dashboardText = strings.dashboardTitle,
                    journeyText = strings.journeyTitle,
                    settingsText = strings.settingsTitle,
                    showJourney = betaEnabled,
                    backdrop = barBackdrop,
                    onNavigate = { route ->
                        bottomNavController.navigate(route) {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(betaEnabled, currentRoute) {
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
    backdrop: Backdrop? = null,
    onNavigate: (String) -> Unit
) {
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
    val barSurfaceBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val barWidthRatio = if (showJourney) 0.88f else 0.72f
        val barWidth = (maxWidth * barWidthRatio).coerceIn(
            minimumValue = if (showJourney) 288.dp else 232.dp,
            maximumValue = if (showJourney) 420.dp else 320.dp
        )
        val barHeight = (maxWidth * 0.165f).coerceIn(64.dp, 72.dp)
        val innerPadding = 5.dp
        val itemSpacing = 6.dp
        val itemCount = navItems.size
        val itemWidth = (barWidth - innerPadding * 2 - itemSpacing * (itemCount - 1)) / itemCount
        val outerRadius = barHeight / 2f
        val innerRadius = (barHeight - innerPadding * 2) / 2f
        val iconSize = (barHeight * 0.375f).coerceIn(24.dp, 28.dp)
        val labelSize = (barHeight.value * 0.18f).coerceIn(11.5f, 13f).sp

        val indicatorOffset by animateDpAsState(
            targetValue = (itemWidth + itemSpacing) * selectedIndex,
            animationSpec = Motion.BottomBarSpring,
            label = "indicator"
        )

        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .glassSurface(
                    level = GlassLevel.FloatingBar,
                    cornerRadius = outerRadius,
                    backdropOverride = backdrop,
                    exportedBackdrop = barSurfaceBackdrop
                )
                .padding(innerPadding)
        ) {
            // Liquid lens pill floating over the bar's exported backdrop
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .glassSurface(
                        level = GlassLevel.ActiveTabIndicator,
                        cornerRadius = innerRadius,
                        backdropOverride = barSurfaceBackdrop
                    )
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
                        iconSize = iconSize,
                        labelSize = labelSize,
                        cornerRadius = innerRadius,
                        onClick = { onNavigate(item.first) }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    textColor: Color,
    itemWidth: Dp,
    iconSize: Dp = 24.dp,
    labelSize: TextUnit = 12.sp,
    cornerRadius: Dp = 26.dp,
    onClick: () -> Unit
) {
    val contentColor = if (selected) textColor else textColor.copy(alpha = 0.65f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
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
            .clip(RoundedCornerShape(cornerRadius))
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
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = labelSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
