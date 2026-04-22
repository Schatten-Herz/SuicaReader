package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.LiquidBackground

import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor

import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.animateDpAsState


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CardDetailsScreen(
    cardIdm: String,
    viewModel: MainViewModel,
    themeViewModel: com.example.suicareader.ui.theme.ThemeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.idm == cardIdm }
    
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    val baseColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F7)
    
    var showManualEntry by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (showManualEntry) 24.dp else 0.dp,
        label = "blur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground(baseColor = baseColor)

        Column(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            Spacer(modifier = Modifier.height(48.dp))
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }

            with(sharedTransitionScope) {
                GlassCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = "card-$cardIdm"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    onClick = { /* 可做翻转动画 */ }
                ) {
                    Column {
                        Text(
                            text = card?.nickname ?: "Unknown Card",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Text(
                            text = "¥${card?.balance ?: 0}",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState(key = "balance_$cardIdm"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                }
            }

            Text(
                text = strings.tripHistory,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val trips by viewModel.getTripsForCard(cardIdm).collectAsState(initial = emptyList())

            // 行程记录瀑布流
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips.size) { index ->
                    val trip = trips[index]
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val dateString = dateFormat.format(java.util.Date(trip.timestamp))
                    
                    val transactionName = when (trip.type) {
                        0x01 -> "Fare (Subway)"
                        0x02 -> "Charge (Top-up)"
                        0x0F -> "Bus Fare"
                        0x46 -> "Purchase (Vending/Store)"
                        else -> "Transaction (0x${"%02X".format(trip.type)})"
                    }
                    
                    val amountColor = if (trip.amount > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    val amountPrefix = if (trip.amount > 0) "+" else ""
                    
                    val inDisplay = trip.inStationName ?: trip.inStation
                    val outDisplay = trip.outStationName ?: trip.outStation

                    val detailText = when (trip.type) {
                        0x02 -> "Location: $inDisplay" // Charge
                        0x0F -> "Bus Route/ID: $inDisplay" // Bus
                        0x46 -> "Terminal: $inDisplay" // Purchase
                        else -> "In: $inDisplay\nOut: $outDisplay" // Subway/Train
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(dateString, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(transactionName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(detailText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 2)
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$amountPrefix¥${trip.amount}", color = amountColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Balance: ¥${trip.balance}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showManualEntry) {
            com.example.suicareader.ui.components.ManualEntryDialog(
                onDismiss = { showManualEntry = false },
                onSubmit = { type, amount, inStationCode, inStationName, outStationCode, outStationName ->
                    viewModel.addManualTrip(cardIdm, type, amount, inStationCode, inStationName, outStationCode, outStationName)
                    showManualEntry = false
                }
            )
        }

        // FAB to add manual trip
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showManualEntry = true },
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
