package com.example.suicareader.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.suicareader.ui.screens.CardDetailsScreen
import com.example.suicareader.ui.screens.DashboardScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.screens.MainScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.suicareader.ui.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(viewModel: MainViewModel, themeViewModel: ThemeViewModel = viewModel()) {
    val navController = rememberNavController()
    
    val currentLang by themeViewModel.currentLanguage.collectAsState()
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    
    val strings = when (currentLang) {
        AppLanguage.EN -> EnStrings
        AppLanguage.ZH -> ZhStrings
        AppLanguage.JA -> JaStrings
    }
    
    val textColor = if (isDark) Color.White else Color(0xFF1E1E1E)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalTextColor provides textColor
    ) {
        SharedTransitionLayout {
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        viewModel = viewModel,
                        themeViewModel = themeViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        onCardClick = { cardIdm ->
                            navController.navigate("details/$cardIdm")
                        }
                    )
                }
            composable(
                route = "details/{cardIdm}",
                arguments = listOf(navArgument("cardIdm") { type = NavType.StringType })
            ) { backStackEntry ->
                val cardIdm = backStackEntry.arguments?.getString("cardIdm") ?: return@composable
                CardDetailsScreen(
                    cardIdm = cardIdm,
                    viewModel = viewModel,
                    themeViewModel = themeViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            }
        }
    }
}
