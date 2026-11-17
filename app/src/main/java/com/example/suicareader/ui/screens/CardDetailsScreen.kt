package com.example.suicareader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.GlassDialogPanel
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.GlassOverlayContainer
import com.example.suicareader.ui.components.ManualEntryDialog
import com.example.suicareader.ui.components.TripUiFormatter
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalOverlayBackdrop
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.example.suicareader.ui.theme.ThemeViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CardDetailsScreen(
    cardIdm: String,
    viewModel: MainViewModel,
    themeViewModel: ThemeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onTripClick: (TripRecord) -> Unit
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.idm == cardIdm }

    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    var showManualEntry by remember { mutableStateOf(false) }
    var showTransferMenu by remember { mutableStateOf(false) }

    val dbTrips by viewModel.getTripsForCard(cardIdm).collectAsState(initial = emptyList())

    var currentTrips by remember { mutableStateOf<List<TripRecord>>(emptyList()) }
    var pendingClickTripId by remember { mutableStateOf<Long?>(null) }
    var enableReorder by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val success = runCatching {
                val json = withContext(Dispatchers.IO) { viewModel.buildTripExportJson(cardIdm) }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open output stream")
            }.isSuccess
            operationMessage = if (success) strings.tripExportSuccess else strings.tripExportFailed
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                operationMessage = strings.tripImportInvalid
                return@launch
            }
            viewModel.importTripsFromJson(cardIdm, text) { success ->
                operationMessage = if (success) strings.tripImportSuccess else strings.tripImportFailed
            }
        }
    }

    LaunchedEffect(dbTrips) {
        currentTrips = dbTrips
    }

    LaunchedEffect(Unit) {
        delay(220)
        enableReorder = true
    }

    val lazyListState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val flattenedList = remember(currentTrips) {
        val list = mutableListOf<Any>()
        val grouped = currentTrips.groupBy { dateFormat.format(Date(it.timestamp)) }
        grouped.forEach { (date, trips) ->
            list.add(date)
            list.addAll(trips)
        }
        list
    }

    val reorderableState = if (enableReorder) {
        rememberReorderableLazyColumnState(lazyListState) { from, to ->
            val fromItem = from.key as? String ?: return@rememberReorderableLazyColumnState
            val toItem = to.key as? String ?: return@rememberReorderableLazyColumnState

            if (fromItem.startsWith("trip_") && toItem.startsWith("trip_")) {
                val fromId = fromItem.removePrefix("trip_").toLongOrNull()
                val toId = toItem.removePrefix("trip_").toLongOrNull()

                if (fromId != null && toId != null) {
                    val fromTrip = currentTrips.find { it.id == fromId }
                    val toTrip = currentTrips.find { it.id == toId }

                    if (fromTrip != null && toTrip != null &&
                        dateFormat.format(Date(fromTrip.timestamp)) == dateFormat.format(Date(toTrip.timestamp))
                    ) {
                        val fromIndex = currentTrips.indexOfFirst { it.id == fromId }
                        val toIndex = currentTrips.indexOfFirst { it.id == toId }
                        if (fromIndex != -1 && toIndex != -1) {
                            currentTrips = currentTrips.toMutableList().apply {
                                add(toIndex, removeAt(fromIndex))
                            }
                        }
                    }
                }
            }
        }
    } else {
        null
    }

    val appBackdrop = LocalAppBackdrop.current
    val pageBackdrop = rememberLayerBackdrop()
    val overlayBackdrop = if (appBackdrop != null) {
        rememberCombinedBackdrop(appBackdrop, pageBackdrop)
    } else {
        pageBackdrop
    }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthDp = maxWidth
        val maxCardHeightDp = (maxWidthDp - 32.dp) / 1.586f
        val minCardHeightDp = 120.dp
        val maxScrollPx = with(density) { (maxCardHeightDp - minCardHeightDp).toPx() }

        val scrollOffsetPx by remember(maxScrollPx) {
            derivedStateOf {
                if (lazyListState.firstVisibleItemIndex == 0) {
                    lazyListState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    maxScrollPx
                }
            }
        }

        val rawFraction = if (maxScrollPx > 0) scrollOffsetPx / maxScrollPx else 0f
        val collapseFraction = rawFraction.coerceIn(0f, 1f)
        val showTripHintCard by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 56
            }
        }
        var hintCardHeightPx by remember { mutableIntStateOf(0) }
        val hintCardHeightDp = with(density) { hintCardHeightPx.toDp() }
        val reservedHintHeight = max(hintCardHeightDp.value, 56f).dp
        val headerOverlayHeight = 110.dp + maxCardHeightDp + reservedHintHeight + 72.dp

        val targetCardHeightDp = lerp(maxCardHeightDp, minCardHeightDp, collapseFraction)
        val currentCardHeightDp by animateDpAsState(
            targetValue = targetCardHeightDp,
            label = "card_height"
        )

        Box(modifier = Modifier.fillMaxSize().layerBackdrop(pageBackdrop)) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = headerOverlayHeight - 40.dp,
                    bottom = 112.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(flattenedList, key = { item ->
                    when (item) {
                        is String -> "header_$item"
                        is TripRecord -> "trip_${item.id}"
                        else -> item.hashCode().toString()
                    }
                }) { item ->
                    when (item) {
                        is String -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item,
                                    color = textColor.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
                                )
                                HorizontalDivider(
                                    color = textColor.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        is TripRecord -> {
                            val handleTripClick = {
                                if (pendingClickTripId == null) {
                                    pendingClickTripId = item.id
                                    scope.launch {
                                        delay(110)
                                        onTripClick(item)
                                        pendingClickTripId = null
                                    }
                                }
                            }
                            if (enableReorder && reorderableState != null) {
                                ReorderableItem(reorderableState, key = "trip_${item.id}") { isDragging ->
                                    TripHistoryCard(
                                        item = item,
                                        isDragging = isDragging,
                                        isPending = pendingClickTripId == item.id,
                                        modifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { },
                                            onDragStopped = {
                                                val dateStr = dateFormat.format(Date(item.timestamp))
                                                val dayTrips = currentTrips.filter {
                                                    dateFormat.format(Date(it.timestamp)) == dateStr
                                                }
                                                viewModel.reorderTripsWithinDay(cardIdm, dayTrips)
                                            }
                                        ),
                                        onClick = handleTripClick
                                    )
                                }
                            } else {
                                TripHistoryCard(
                                    item = item,
                                    isDragging = false,
                                    isPending = pendingClickTripId == item.id,
                                    onClick = handleTripClick
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Header Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerOverlayHeight)
            ) {
                if (collapseFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF070B12).copy(alpha = collapseFraction * 0.82f),
                                        Color(0xFF070B12).copy(alpha = collapseFraction * 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 21.dp)
                                .clickable {
                                    scope.launch {
                                        if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
                                            lazyListState.animateScrollToItem(0, 0)
                                            delay(120)
                                        }
                                        onBackClick()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = strings.back, tint = textColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 21.dp)
                                .clickable { showTransferMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ImportExport, contentDescription = strings.importExportTitle, tint = textColor)
                        }
                    }

                    with(sharedTransitionScope) {
                        GlassCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .height(currentCardHeightDp)
                                .sharedBounds(
                                    rememberSharedContentState(key = "card-$cardIdm"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            level = GlassLevel.SurfacePrimary,
                            onClick = { }
                        ) {
                            val expandedBalanceSize = 48.sp
                            val expandedNicknameSize = expandedBalanceSize / 1.618f
                            val collapsedBalanceSize = 28.sp
                            val collapsedNicknameSize = collapsedBalanceSize / 1.618f

                            val nicknameSize = lerp(expandedNicknameSize, collapsedNicknameSize, collapseFraction)
                            val balanceSize = lerp(expandedBalanceSize, collapsedBalanceSize, collapseFraction)

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = card?.nickname ?: strings.unknownCard,
                                    color = Color.White,
                                    fontSize = nicknameSize,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.sharedBounds(
                                        rememberSharedContentState(key = "nickname_${cardIdm}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )

                                Text(
                                    text = "¥${card?.balance ?: 0}",
                                    color = Color.White,
                                    fontSize = balanceSize,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.sharedBounds(
                                        rememberSharedContentState(key = "balance_$cardIdm"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showTripHintCard,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                                .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
                                .onSizeChanged { hintCardHeightPx = it.height }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${strings.lastUpdatedLabel}: ${dateTimeFormat.format(Date(card?.lastUpdated ?: System.currentTimeMillis()))}",
                                    color = textColor.copy(alpha = 0.92f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = strings.tripCapacityHint,
                                    color = textColor.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    val textAlpha = (1f - (collapseFraction * 2f)).coerceIn(0f, 1f)
                    val titleTranslateY = lerp(0.dp, (-10).dp, collapseFraction)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .graphicsLayer {
                                alpha = textAlpha
                                translationY = with(density) { titleTranslateY.toPx() }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = strings.tripHistory,
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        operationMessage?.let { msg ->
            LaunchedEffect(msg) {
                delay(2200)
                operationMessage = null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .glassSurface(
                            level = GlassLevel.Overlay,
                            cornerRadius = 16.dp,
                            backdropOverride = overlayBackdrop
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Floating Liquid Glass '+' Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .glassSurface(
                        level = GlassLevel.Overlay,
                        cornerRadius = 29.dp,
                        backdropOverride = overlayBackdrop
                    )
                    .clickable { showManualEntry = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.addManualEntry,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        CompositionLocalProvider(LocalOverlayBackdrop provides appBackdrop) {
            if (showTransferMenu) {
                GlassOverlayContainer(onDismissRequest = { showTransferMenu = false }) {
                    GlassDialogPanel(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .widthIn(max = 420.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                strings.importExportTitle,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 16.dp)
                                    .clickable {
                                        showTransferMenu = false
                                        importLauncher.launch("application/json")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 15.dp)
                            ) {
                                Text(
                                    text = strings.importTrips,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 16.dp)
                                    .clickable {
                                        showTransferMenu = false
                                        val name = (card?.nickname?.ifBlank { "card" } ?: "card")
                                            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                        exportLauncher.launch("${name}_trips.suica.json")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 15.dp)
                            ) {
                                Text(
                                    text = strings.exportCurrentTrips,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (showManualEntry) {
                ManualEntryDialog(
                    onDismiss = { showManualEntry = false },
                    onSubmit = { type, amount, inStationCode, inStationName, outStationCode, outStationName, timestamp, customTitle, note ->
                        viewModel.addManualTrip(
                            cardIdm,
                            type,
                            amount,
                            inStationCode,
                            inStationName,
                            outStationCode,
                            outStationName,
                            timestamp,
                            customTitle,
                            note
                        )
                        showManualEntry = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TripHistoryCard(
    item: TripRecord,
    isDragging: Boolean,
    isPending: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val transactionName = TripUiFormatter.transactionTitle(item.type, strings)

    val amountColor = if (item.amount > 0) Color(0xFF4ADE80) else Color(0xFFFF6B6B)
    val amountPrefix = if (item.amount > 0) "+" else ""

    val detailText = TripUiFormatter.tripSummary(item, strings)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed || isDragging || isPending) 0.96f else 1f,
        label = "trip_press_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .glassSurface(
                level = GlassLevel.SurfaceSecondary,
                cornerRadius = 18.dp,
                isPressed = isPressed || isDragging
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.customTitle ?: transactionName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    detailText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$amountPrefix¥${item.amount}",
                    color = amountColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${strings.balanceLabel} ¥${item.balance}",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
