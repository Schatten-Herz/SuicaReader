package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.data.db.entity.TransitCard
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.GlassConfirmDialog
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.GlassMenuDialog
import com.example.suicareader.ui.components.RenameCardDialog
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalOverlayBackdrop
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: (String) -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    val cardToEdit = remember { mutableStateOf<TransitCard?>(null) }
    val cardToDelete = remember { mutableStateOf<String?>(null) }

    val appBackdrop = LocalAppBackdrop.current
    val overlayBackdrop = appBackdrop

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.dashboardTitle,
                    color = textColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 16.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "NFC",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 108.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        level = GlassLevel.SurfacePrimary,
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = strings.noCardsTitle,
                                color = textColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.noCardsDesc,
                                color = textColor.copy(alpha = 0.78f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 108.dp)
                ) {
                    items(cards.size) { i ->
                        val card = cards[i]
                    with(sharedTransitionScope) {
                        GlassCard(
                            modifier = Modifier
                                .aspectRatio(1.586f)
                                .sharedBounds(
                                    rememberSharedContentState(key = "card-${card.idm}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            level = GlassLevel.SurfacePrimary,
                            onClick = { onCardClick(card.idm) },
                            onLongClick = { cardToEdit.value = card }
                        ) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val nicknameSize = (maxWidth.value * 0.092f).coerceIn(19f, 27f).sp
                                val balanceSize = (maxWidth.value * 0.128f).coerceIn(30f, 42f).sp
                                val numberSize = (maxWidth.value * 0.042f).coerceIn(12f, 15f).sp
                                val topPadding = (maxHeight * 0.08f).coerceIn(8.dp, 18.dp)
                                val nameBottomGap = (maxHeight * 0.03f).coerceIn(4.dp, 10.dp)
                                val showNumber = !card.displayNumber.isNullOrBlank() && maxHeight > 140.dp

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Spacer(modifier = Modifier.height(topPadding))

                                    Text(
                                        text = card.nickname,
                                        color = Color.White,
                                        fontSize = nicknameSize,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = nicknameSize * 1.08f,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "nickname_${card.idm}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(nameBottomGap))

                                    if (showNumber) {
                                        Text(
                                            text = "•••• •••• •••• ${card.displayNumber}",
                                            color = Color.White.copy(alpha = 0.72f),
                                            fontSize = numberSize,
                                            letterSpacing = 2.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    with(sharedTransitionScope) {
                                        Text(
                                            text = "¥${card.balance}",
                                            color = Color.White,
                                            fontSize = balanceSize,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "balance_${card.idm}"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        CompositionLocalProvider(LocalOverlayBackdrop provides overlayBackdrop) {
            val editingCard = cardToEdit.value
            if (editingCard != null) {
                val isMenuMode = remember(editingCard.idm) { mutableStateOf(true) }

                if (isMenuMode.value) {
                    GlassMenuDialog(
                        onDismissRequest = { cardToEdit.value = null },
                        onEditClick = { isMenuMode.value = false },
                        onDeleteClick = {
                            cardToDelete.value = editingCard.idm
                            cardToEdit.value = null
                        },
                        title = strings.cardOptionsTitle,
                        editText = strings.editCard,
                        deleteText = strings.deleteCard
                    )
                } else {
                    RenameCardDialog(
                        initialName = editingCard.nickname,
                        initialNumber = editingCard.displayNumber ?: "",
                        onDismissRequest = { cardToEdit.value = null },
                        onSaveClick = { newName, newNumber ->
                            viewModel.updateCardInfo(editingCard.idm, newName, newNumber)
                            cardToEdit.value = null
                        }
                    )
                }
            }

            val deletingIdm = cardToDelete.value
            if (deletingIdm != null) {
                GlassConfirmDialog(
                    title = strings.deleteCardTitle,
                    message = strings.deleteCardMessage,
                    confirmText = strings.deleteCard,
                    confirmColor = Color(0xFFFF6B6B),
                    onDismissRequest = { cardToDelete.value = null },
                    onConfirmClick = {
                        viewModel.deleteCard(deletingIdm)
                        cardToDelete.value = null
                    }
                )
            }
        }
    }
}
