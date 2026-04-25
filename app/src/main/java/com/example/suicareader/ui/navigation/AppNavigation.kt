package com.example.suicareader.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.suicareader.ui.screens.CardDetailsScreen
import com.example.suicareader.ui.screens.DashboardScreen
import com.example.suicareader.ui.screens.TripDetailsScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.screens.MainScreen
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.suicareader.ui.theme.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.suicareader.ui.components.LiquidBackground

private object Routes {
    const val Main = "main"
    const val Details = "details/{cardIdm}"
    const val TripDetails = "trip-details/{cardIdm}/{tripId}"
    fun details(cardIdm: String) = "details/$cardIdm"
    fun tripDetails(cardIdm: String, tripId: Long) = "trip-details/$cardIdm/$tripId"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(viewModel: MainViewModel, themeViewModel: ThemeViewModel = viewModel()) {
    val navController = rememberNavController()
    
    val currentLang by themeViewModel.currentLanguage.collectAsState()
    
    val strings = when (currentLang) {
        AppLanguage.EN -> EnStrings
        AppLanguage.ZH -> ZhStrings
        AppLanguage.JA -> JaStrings
    }
    
    val textColor = Color.White

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalTextColor provides textColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidBackground(baseColor = Color(0xFF1E1E1E))
            SharedTransitionLayout {
                NavHost(navController = navController, startDestination = Routes.Main) {
                    composable(
                        route = Routes.Main,
                        enterTransition = { fadeIn(animationSpec = Motion.NavFadeIn) },
                        exitTransition = { fadeOut(animationSpec = Motion.NavFadeOut) },
                        popEnterTransition = { fadeIn(animationSpec = Motion.NavFadeIn) },
                        popExitTransition = { fadeOut(animationSpec = Motion.NavFadeOut) }
                    ) {
                        MainScreen(
                            viewModel = viewModel,
                            themeViewModel = themeViewModel,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            onCardClick = { cardIdm ->
                                navController.navigate(Routes.details(cardIdm))
                            }
                        )
                    }

                    composable(
                        route = Routes.Details,
                        arguments = listOf(navArgument("cardIdm") { type = NavType.StringType }),
                        enterTransition = {
                            fadeIn(animationSpec = Motion.NavFadeIn) + slideInVertically(
                                animationSpec = Motion.NavEnter,
                                initialOffsetY = { it / 10 }
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = Motion.NavFadeOut) + slideOutVertically(
                                animationSpec = Motion.NavExit,
                                targetOffsetY = { -it / 12 }
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = Motion.NavFadeIn) + slideInVertically(
                                animationSpec = Motion.NavEnter,
                                initialOffsetY = { -it / 14 }
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = Motion.NavFadeOut) + slideOutVertically(
                                animationSpec = Motion.NavExit,
                                targetOffsetY = { it / 10 }
                            )
                        }
                    ) { backStackEntry ->
                        val cardIdm = backStackEntry.arguments?.getString("cardIdm") ?: return@composable
                        CardDetailsScreen(
                            cardIdm = cardIdm,
                            viewModel = viewModel,
                            themeViewModel = themeViewModel,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            onBackClick = { navController.popBackStack() },
                            onTripClick = { trip ->
                                navController.navigate(Routes.tripDetails(trip.cardIdm, trip.id))
                            }
                        )
                    }

                    composable(
                        route = Routes.TripDetails,
                        arguments = listOf(
                            navArgument("cardIdm") { type = NavType.StringType },
                            navArgument("tripId") { type = NavType.LongType }
                        ),
                        enterTransition = {
                            fadeIn(animationSpec = Motion.NavFadeIn) + slideInVertically(
                                animationSpec = Motion.NavEnter,
                                initialOffsetY = { it / 8 }
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = Motion.NavFadeOut) + slideOutVertically(
                                animationSpec = Motion.NavExit,
                                targetOffsetY = { -it / 10 }
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = Motion.NavFadeIn) + slideInVertically(
                                animationSpec = Motion.NavEnter,
                                initialOffsetY = { -it / 12 }
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = Motion.NavFadeOut) + slideOutVertically(
                                animationSpec = Motion.NavExit,
                                targetOffsetY = { it / 8 }
                            )
                        }
                    ) { backStackEntry ->
                        val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
                        val trip by viewModel.getTripById(tripId).collectAsState(initial = null)
                        TripDetailsScreen(
                            trip = trip,
                            onSaveEdit = { source, title, note ->
                                viewModel.updateTripDetails(source, title, note)
                            },
                            onDeleteTrip = { source ->
                                viewModel.deleteTrip(source)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
