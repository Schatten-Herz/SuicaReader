package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.data.db.entity.TransitCard
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.LiquidBackground
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.suicareader.ui.MainViewModel

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: (String) -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val cardToEdit = remember { mutableStateOf<com.example.suicareader.data.db.entity.TransitCard?>(null) }
    val cardToDelete = remember { mutableStateOf<String?>(null) } // Used for confirmation dialog

    val isDialogOpen = cardToEdit.value != null || cardToDelete.value != null
    val blurRadius by animateDpAsState(
        targetValue = if (isDialogOpen) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "dialog_blur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            LiquidBackground()

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "My Cards",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cards) { card ->
                        with(sharedTransitionScope) {
                            GlassCard(
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "card-${card.idm}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                                onClick = { onCardClick(card.idm) },
                                onLongClick = { cardToEdit.value = card } // Changed to show menu
                            ) {
                                Column {
                                    Text(
                                        text = card.nickname,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!card.displayNumber.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "•••• •••• •••• ${card.displayNumber}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "¥${card.balance}",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // --- End of blurred content Box ---

            if (cardToEdit.value != null) {
                val isMenuMode = remember { mutableStateOf(true) }

                if (isMenuMode.value) {
                    com.example.suicareader.ui.components.GlassMenuDialog(
                        onDismissRequest = { cardToEdit.value = null },
                        onEditClick = {
                            isMenuMode.value = false
                        },
                        onDeleteClick = {
                            cardToDelete.value = cardToEdit.value!!.idm
                            cardToEdit.value = null
                        }
                    )
                } else {
                    com.example.suicareader.ui.components.RenameCardDialog(
                        initialName = cardToEdit.value!!.nickname,
                        initialNumber = cardToEdit.value!!.displayNumber ?: "",
                        onDismissRequest = { cardToEdit.value = null },
                        onSaveClick = { newName, newNumber ->
                            viewModel.updateCardInfo(cardToEdit.value!!.idm, newName, newNumber)
                            cardToEdit.value = null
                        }
                    )
                }
            }

            if (cardToDelete.value != null) {
                com.example.suicareader.ui.components.GlassConfirmDialog(
                    title = "Delete Card?",
                    message = "This will permanently delete the card and all its trip history. This action cannot be undone.",
                    confirmText = "Delete",
                    confirmColor = Color(0xFFFF5252),
                    onDismissRequest = { cardToDelete.value = null },
                    onConfirmClick = {
                        viewModel.deleteCard(cardToDelete.value!!)
                        cardToDelete.value = null
                    }
                )
            }
        }
    }
}