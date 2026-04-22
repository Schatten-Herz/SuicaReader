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

import com.example.suicareader.ui.MainViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    SharedTransitionLayout {
        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
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
