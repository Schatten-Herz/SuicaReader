package com.example.suicareader.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.LiquidBackground
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.screens.CardDetailsScreen
import com.example.suicareader.ui.screens.MainScreen
import com.example.suicareader.ui.screens.TripDetailsScreen
import com.example.suicareader.ui.theme.*
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay

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
    val blurPreset by themeViewModel.blurPreset.collectAsState()
    val nfcFeedback by viewModel.nfcFeedback.collectAsState()

    LaunchedEffect(nfcFeedback?.id) {
        val current = nfcFeedback ?: return@LaunchedEffect
        if (current.status != MainViewModel.NfcStatus.Reading) {
            delay(2600)
            viewModel.clearNfcFeedback()
        }
    }
    
    val strings = when (currentLang) {
        AppLanguage.EN -> EnStrings
        AppLanguage.ZH -> ZhStrings
        AppLanguage.JA -> JaStrings
    }
    
    val textColor = Color.White
    
    val appBackdrop = rememberLayerBackdrop()

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalTextColor provides textColor,
        LocalBlurPreset provides blurPreset,
        LocalAppBackdrop provides appBackdrop
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.layerBackdrop(appBackdrop).fillMaxSize()) {
                LiquidBackground(baseColor = Color(0xFF15803D))
            }
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
                            onSaveEdit = { source, title, note, timestamp, amount, inCode, inName, outCode, outName ->
                                viewModel.updateTripDetails(
                                    trip = source,
                                    newTitle = title,
                                    newNote = note,
                                    newTimestamp = timestamp,
                                    newAmount = amount,
                                    newInStationCode = inCode,
                                    newInStationName = inName,
                                    newOutStationCode = outCode,
                                    newOutStationName = outName
                                )
                            },
                            onDeleteTrip = { source ->
                                viewModel.deleteTrip(source)
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = nfcFeedback != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .zIndex(100f)
            ) {
                nfcFeedback?.let { event ->
                    val (tint, label) = when (event.status) {
                        MainViewModel.NfcStatus.Reading -> Color(0xFF38BDF8) to strings.nfcReading
                        MainViewModel.NfcStatus.Success -> Color(0xFF4ADE80) to "${event.cardName ?: strings.nfcReadSuccess} · ¥${event.balance ?: 0}"
                        MainViewModel.NfcStatus.TagLost -> Color(0xFFFBBF24) to strings.nfcTagLost
                        MainViewModel.NfcStatus.Unsupported -> Color(0xFFFF6B6B) to strings.nfcUnsupported
                        MainViewModel.NfcStatus.Failed -> Color(0xFFFF6B6B) to strings.nfcReadFailed
                    }
                    Row(
                        modifier = Modifier
                            .glassSurface(level = GlassLevel.Overlay, cornerRadius = 24.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (event.status) {
                            MainViewModel.NfcStatus.Reading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = tint,
                                    strokeWidth = 2.dp
                                )
                            }
                            MainViewModel.NfcStatus.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            MainViewModel.NfcStatus.TagLost,
                            MainViewModel.NfcStatus.Unsupported,
                            MainViewModel.NfcStatus.Failed -> {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
