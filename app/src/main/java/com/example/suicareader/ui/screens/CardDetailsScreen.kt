package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import sh.calvin.reorderable.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CardDetailsScreen(
    cardIdm: String,
    viewModel: MainViewModel,
    themeViewModel: com.example.suicareader.ui.theme.ThemeViewModel,
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
    val blurRadius by animateDpAsState(
        targetValue = if (showManualEntry || showTransferMenu) 40.dp else 0.dp,
        label = "blur"
    )

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
    
    // Flattened list for the LazyColumn
    val flattenedList = remember(currentTrips) {
        val list = mutableListOf<Any>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val grouped = currentTrips.groupBy { dateFormat.format(Date(it.timestamp)) }
        grouped.forEach { (date, trips) ->
            list.add(date) // Date Header
            list.addAll(trips)
        }
        list
    }

    val reorderableState = if (enableReorder) {
        rememberReorderableLazyColumnState(lazyListState) { from, to ->
            val fromItem = from.key as? String ?: return@rememberReorderableLazyColumnState
            val toItem = to.key as? String ?: return@rememberReorderableLazyColumnState
            
            // Key format is "trip_${id}" or "header_${date}"
            if (fromItem.startsWith("trip_") && toItem.startsWith("trip_")) {
                val fromId = fromItem.removePrefix("trip_").toLongOrNull()
                val toId = toItem.removePrefix("trip_").toLongOrNull()
                
                if (fromId != null && toId != null) {
                    val fromTrip = currentTrips.find { it.id == fromId }
                    val toTrip = currentTrips.find { it.id == toId }
                    
                    if (fromTrip != null && toTrip != null) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        if (dateFormat.format(Date(fromTrip.timestamp)) == dateFormat.format(Date(toTrip.timestamp))) {
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
        }
    } else {
        null
    }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthDp = maxWidth
        val maxCardHeightDp = (maxWidthDp - 32.dp) / 1.586f // ISO/IEC 7810 ID-1 ratio
        val minCardHeightDp = 120.dp // Increased to prevent clipping of balance text
        val maxScrollPx = with(density) { (maxCardHeightDp - minCardHeightDp).toPx() }
        
        // Calculate collapse fraction (0f = expanded, 1f = collapsed)
        // Use derived state to avoid unnecessary recompositions if only scroll changes
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
        
        val contentModifier = if (blurRadius > 0.dp) Modifier.fillMaxSize().blur(blurRadius) else Modifier.fillMaxSize()
        Box(modifier = contentModifier) {
            
            // Trip History List
            LazyColumn(
                state = lazyListState,
                // Pad the top so the first item starts below the expanded header
                contentPadding = PaddingValues(
                    top = headerOverlayHeight - 40.dp,
                    bottom = 100.dp, 
                    start = 16.dp, 
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Background spacer inside the list is NOT needed since we use padding
                
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
                                    color = textColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 0.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                                )
                                HorizontalDivider(color = textColor.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                        is TripRecord -> {
                            if (enableReorder && reorderableState != null) {
                                ReorderableItem(reorderableState, key = "trip_${item.id}") { isDragging ->
                                    val transactionName = when (item.type) {
                                        0x01 -> strings.fareSubway
                                        0x02 -> strings.chargeTopUp
                                        0x0F, 0x0D -> strings.busFare
                                        0x46 -> strings.purchase
                                        0x50 -> strings.typeLocker
                                        else -> "${strings.transactionLabel} (0x${"%02X".format(item.type)})"
                                    }
                                    
                                    val amountColor = if (item.amount > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                    val amountPrefix = if (item.amount > 0) "+" else ""
                                    
                                    val inDisplay = item.inStationName ?: item.inStation
                                    val outDisplay = item.outStationName ?: item.outStation

                                    val detailText = when (item.type) {
                                        0x02 -> "${strings.locationPrefix} $inDisplay"
                                        0x0F, 0x0D -> "${strings.busRoutePrefix} $inDisplay"
                                        0x46 -> "${strings.terminalPrefix} $inDisplay"
                                        0x50 -> item.note?.takeIf { it.isNotBlank() } ?: strings.lockerUsage
                                        else -> "${strings.inPrefix} $inDisplay\n${strings.outPrefix} $outDisplay"
                                    }

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val pressScale by animateFloatAsState(
                                        targetValue = if (isPressed || isDragging || pendingClickTripId == item.id) 0.95f else 1f,
                                        label = "trip_press_scale"
                                    )

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(pressScale)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                if (pendingClickTripId != null) return@clickable
                                                pendingClickTripId = item.id
                                                scope.launch {
                                                    delay(110)
                                                    onTripClick(item)
                                                    pendingClickTripId = null
                                                }
                                            }
                                            .longPressDraggableHandle(
                                                onDragStarted = { },
                                                onDragStopped = {
                                                    // Save changes to DB when drag stops
                                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                    val dateStr = dateFormat.format(Date(item.timestamp))
                                                    val dayTrips = currentTrips.filter { dateFormat.format(Date(it.timestamp)) == dateStr }
                                                    viewModel.reorderTripsWithinDay(cardIdm, dayTrips)
                                                }
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(alpha = if (isDragging) 0.3f else 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.customTitle ?: transactionName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(detailText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 2)
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("$amountPrefix¥${item.amount}", color = amountColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("${strings.balanceLabel} ¥${item.balance}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                val transactionName = when (item.type) {
                                    0x01 -> strings.fareSubway
                                    0x02 -> strings.chargeTopUp
                                    0x0F, 0x0D -> strings.busFare
                                    0x46 -> strings.purchase
                                    0x50 -> strings.typeLocker
                                    else -> "${strings.transactionLabel} (0x${"%02X".format(item.type)})"
                                }
                                
                                val amountColor = if (item.amount > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                val amountPrefix = if (item.amount > 0) "+" else ""
                                
                                val inDisplay = item.inStationName ?: item.inStation
                                val outDisplay = item.outStationName ?: item.outStation

                                val detailText = when (item.type) {
                                    0x02 -> "${strings.locationPrefix} $inDisplay"
                                    0x0F, 0x0D -> "${strings.busRoutePrefix} $inDisplay"
                                    0x46 -> "${strings.terminalPrefix} $inDisplay"
                                    0x50 -> item.note?.takeIf { it.isNotBlank() } ?: strings.lockerUsage
                                    else -> "${strings.inPrefix} $inDisplay\n${strings.outPrefix} $outDisplay"
                                }

                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val pressScale by animateFloatAsState(
                                    targetValue = if (isPressed || pendingClickTripId == item.id) 0.95f else 1f,
                                    label = "trip_press_scale"
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(pressScale)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            if (pendingClickTripId != null) return@clickable
                                            pendingClickTripId = item.id
                                            scope.launch {
                                                delay(110)
                                                onTripClick(item)
                                                pendingClickTripId = null
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.customTitle ?: transactionName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(detailText, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 2)
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("$amountPrefix¥${item.amount}", color = amountColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${strings.balanceLabel} ¥${item.balance}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // End of LazyColumn
            
            // Sticky Header Container (Top Overlays)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerOverlayHeight)
            ) {
                // Lightweight top overlay to avoid costly per-frame blur during scroll.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Background for the header to blend smoothly with list
                if (collapseFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF121212).copy(alpha = collapseFraction * 0.8f),
                                        Color(0xFF121212).copy(alpha = collapseFraction * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
                                    lazyListState.animateScrollToItem(0, 0)
                                    delay(120)
                                }
                                onBackClick()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = strings.back, tint = textColor)
                        }
                        IconButton(onClick = { showTransferMenu = true }) {
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
                            onClick = { /* Flip animation placeholder */ }
                        ) {
                            // Golden ratio applied to typography
                            val expandedBalanceSize = 48.sp
                            val expandedNicknameSize = expandedBalanceSize / 1.618f // ~29.6sp
                            val collapsedBalanceSize = 28.sp
                            val collapsedNicknameSize = collapsedBalanceSize / 1.618f // ~17.3sp

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
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                                .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 24.dp)
                                .onSizeChanged { hintCardHeightPx = it.height },
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${strings.lastUpdatedLabel}: ${dateTimeFormat.format(Date(card?.lastUpdated ?: System.currentTimeMillis()))}",
                                    color = textColor.copy(alpha = 0.9f),
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

                    // Trip History Title
                    val textAlpha = (1f - (collapseFraction * 2f)).coerceIn(0f, 1f)
                    val titleTranslateY = lerp(0.dp, (-10).dp, collapseFraction)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 0.dp, bottom = 0.dp)
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.42f)
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (showTransferMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(Color.Black.copy(alpha = 0.26f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showTransferMenu = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(22.dp))
                        .glassSurface(level = GlassLevel.Overlay, cornerRadius = 22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.importExportTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTransferMenu = false
                                    importLauncher.launch("application/json")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = strings.importTrips,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTransferMenu = false
                                    val name = (card?.nickname?.ifBlank { "card" } ?: "card")
                                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                    exportLauncher.launch("${name}_trips.suica.json")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = strings.exportCurrentTrips,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showManualEntry) {
            com.example.suicareader.ui.components.ManualEntryDialog(
                onDismiss = { showManualEntry = false },
                onSubmit = { type, amount, inStationCode, inStationName, outStationCode, outStationName, timestamp, customTitle, note ->
                    viewModel.addManualTrip(cardIdm, type, amount, inStationCode, inStationName, outStationCode, outStationName, timestamp, customTitle, note)
                    showManualEntry = false
                }
            )
        }

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
