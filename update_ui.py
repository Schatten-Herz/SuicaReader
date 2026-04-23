import re
import os

filepath = r"d:\Suica\app\src\main\java\com\example\suicareader\ui\screens\CardDetailsScreen.kt"

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if "Box(modifier = Modifier.fillMaxSize()) {" in line and "Column(modifier = Modifier.fillMaxSize().blur(blurRadius)) {" in lines[i+1]:
        start_idx = i
        break

for i in range(start_idx, len(lines)):
    if "if (showManualEntry) {" in lines[i]:
        end_idx = i - 1
        break

if start_idx != -1 and end_idx != -1:
    new_code = """    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthDp = maxWidth
        val maxCardHeightDp = (maxWidthDp - 32.dp) / 1.586f // ISO/IEC 7810 ID-1 ratio
        val minCardHeightDp = 80.dp
        
        // Calculate collapse fraction (0f = expanded, 1f = collapsed)
        val scrollOffsetPx = if (lazyListState.firstVisibleItemIndex == 0) {
            lazyListState.firstVisibleItemScrollOffset.toFloat()
        } else {
            Float.MAX_VALUE
        }
        
        val maxScrollPx = with(density) { (maxCardHeightDp - minCardHeightDp).toPx() }
        val rawFraction = if (maxScrollPx > 0) scrollOffsetPx / maxScrollPx else 0f
        val collapseFraction = rawFraction.coerceIn(0f, 1f)
        
        val currentCardHeightDp = lerp(maxCardHeightDp, minCardHeightDp, collapseFraction)
        
        Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            
            // Trip History List
            LazyColumn(
                state = lazyListState,
                // Pad the top so the first item starts below the expanded header
                contentPadding = PaddingValues(
                    top = 110.dp + maxCardHeightDp + 32.dp, 
                    bottom = 100.dp, 
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
                                    color = textColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        is TripRecord -> {
                            val isDragging = reorderableState.draggedIndex == lazyListState.layoutInfo.visibleItemsInfo.find { it.key == "trip_${item.id}" }?.index

                            ReorderableItem(reorderableState, key = "trip_${item.id}") {
                                val transactionName = when (item.type) {
                                    0x01 -> strings.typeTrain
                                    0x02 -> strings.typeRecharge
                                    0x0F, 0x0D -> strings.typeBus
                                    0x46 -> strings.typePurchase
                                    else -> strings.typeUnknown
                                }

                                val amountColor = when (item.type) {
                                    0x01, 0x0F, 0x0D, 0x46 -> textColor
                                    0x02 -> Color(0xFF4CAF50)
                                    else -> textColor
                                }

                                val amountPrefix = if (item.amount > 0) "+" else ""

                                val inDisplay = item.inStationName ?: item.inStation
                                val outDisplay = item.outStationName ?: item.outStation

                                val detailText = when (item.type) {
                                    0x02 -> "${strings.locationPrefix} $inDisplay"
                                    0x0F, 0x0D -> "${strings.busRoutePrefix} $inDisplay"
                                    0x46 -> "${strings.terminalPrefix} $inDisplay"
                                    else -> "${strings.inPrefix} $inDisplay\\n${strings.outPrefix} $outDisplay"
                                }

                                val scale by animateFloatAsState(
                                    targetValue = if (isDragging) 0.95f else 1f,
                                    label = "drag_scale"
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(scale)
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
                                            Text(transactionName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Apply a background blur to the header area when collapsed to prevent text overlap
                    .background(Color(0xFF121212).copy(alpha = collapseFraction * 0.7f))
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
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
                        onClick = { /* Flip animation placeholder */ }
                    ) {
                        val nicknameSize = lerp(24.sp, 16.sp, collapseFraction)
                        val balanceSize = lerp(48.sp, 28.sp, collapseFraction)
                        
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = card?.nickname ?: "Unknown Card",
                                color = Color.White,
                                fontSize = nicknameSize,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            // Dynamic spacer that shrinks to 0 when collapsed
                            if (collapseFraction < 1f) {
                                Spacer(modifier = Modifier.weight(1f - collapseFraction))
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
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

                // Trip History Title
                val textAlpha = (1f - (collapseFraction * 2f)).coerceIn(0f, 1f)
                val textHeight = lerp(40.dp, 0.dp, collapseFraction)
                
                if (textAlpha > 0f) {
                    Text(
                        text = strings.tripHistory,
                        color = textColor.copy(alpha = textAlpha),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 8.dp)
                            .height(textHeight)
                    )
                }
            }
        }
"""
    
    lines = lines[:start_idx] + [new_code] + lines[end_idx:]
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    print("Successfully updated CardDetailsScreen.kt")
else:
    print(f"Could not find indices: start={start_idx}, end={end_idx}")

