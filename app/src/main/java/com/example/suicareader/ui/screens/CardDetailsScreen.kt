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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.LiquidBackground

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CardDetailsScreen(
    cardIdm: String,
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.idm == cardIdm }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            with(sharedTransitionScope) {
                GlassCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .sharedElement(
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Trip History (Demo)",
                color = Color.White,
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
                        "01" -> "Fare (Subway)"
                        "02" -> "Charge (Top-up)"
                        "0F" -> "Bus Fare"
                        "46" -> "Purchase (Vending/Store)"
                        else -> "Transaction (0x${trip.type})"
                    }
                    
                    val amountColor = if (trip.amount > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    val amountPrefix = if (trip.amount > 0) "+" else ""
                    
                    val inDisplay = trip.inStationName ?: trip.inStation
                    val outDisplay = trip.outStationName ?: trip.outStation

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
                                    Text("In: $inDisplay\nOut: $outDisplay", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 2)
                                }
                                
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text("$amountPrefix¥${trip.amount}", color = amountColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Balance: ¥${trip.balance}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
