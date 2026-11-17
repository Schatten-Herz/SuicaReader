package com.example.suicareader.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.GlassConfirmDialog
import com.example.suicareader.ui.components.GlassDialogPanel
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.GlassMenuDialog
import com.example.suicareader.ui.components.GlassOverlayContainer
import com.example.suicareader.ui.components.TripUiFormatter
import com.example.suicareader.ui.components.glassOutlinedTextFieldColors
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.map.TransitMapCatalog
import com.example.suicareader.ui.theme.LocalAppBackdrop
import com.example.suicareader.ui.theme.LocalOverlayBackdrop
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.example.suicareader.ui.theme.Motion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TripDetailsScreen(
    trip: TripRecord?,
    onSaveEdit: (TripRecord, String, String, Long, Int) -> Unit,
    onDeleteTrip: (TripRecord) -> Unit,
    onBackClick: () -> Unit
) {
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    var showActionMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingTitle by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.customTitle ?: "") }
    var editingAmount by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.amount?.toString() ?: "0") }
    val editDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedEditDateMillis by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.timestamp ?: System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var editingNote by remember(trip?.id, showEditDialog, showNoteDialog) { mutableStateOf(trip?.note ?: "") }
    var editValidationMessage by remember(trip?.id, showEditDialog) { mutableStateOf<String?>(null) }

    val inStationText = trip?.inStationName ?: trip?.inStation
    val outStationText = trip?.outStationName ?: trip?.outStation
    val startLatLng = remember(inStationText) { TransitMapCatalog.coordinateForStation(inStationText) }
    val endLatLng = remember(outStationText) { TransitMapCatalog.coordinateForStation(outStationText) }
    val startMarkerState = remember(startLatLng) { startLatLng?.let { MarkerState(it) } }
    val endMarkerState = remember(endLatLng) { endLatLng?.let { MarkerState(it) } }
    val lineCompany = remember(inStationText) { TransitMapCatalog.companyName(inStationText) }
    val lineColor = remember(lineCompany) { TransitMapCatalog.colorForCompany(lineCompany) }
    val cameraPositionState = rememberCameraPositionState()
    var showMapContent by remember(trip?.id) { mutableStateOf(false) }
    var stretchOffset by remember { mutableStateOf(0f) }
    val animatedStretch by animateFloatAsState(
        targetValue = stretchOffset,
        animationSpec = Motion.RubberBandLight,
        label = "trip_limit_stretch"
    )
    val noteLength = trip?.note?.length ?: 0
    val notePressure = (noteLength / 180f).coerceIn(0f, 1f)
    val mapWeight by animateFloatAsState(
        targetValue = (3.6f - notePressure * 1.6f).coerceIn(2.0f, 3.6f),
        label = "trip_map_weight"
    )
    val detailsWeight by animateFloatAsState(
        targetValue = 1.9f + notePressure * 0.6f,
        label = "trip_details_weight"
    )

    val appBackdrop = LocalAppBackdrop.current
    val pageBackdrop = rememberLayerBackdrop()
    val overlayBackdrop = if (appBackdrop != null) {
        rememberCombinedBackdrop(appBackdrop, pageBackdrop)
    } else {
        pageBackdrop
    }

    LaunchedEffect(trip?.id) {
        showMapContent = false
        delay(380)
        showMapContent = true
    }

    LaunchedEffect(startLatLng, endLatLng, showMapContent) {
        if (!showMapContent) return@LaunchedEffect
        when {
            startLatLng != null && endLatLng != null -> {
                val bounds = LatLngBounds.builder()
                    .include(startLatLng)
                    .include(endLatLng)
                    .build()
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 140))
            }
            startLatLng != null -> {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(startLatLng, 13.5f))
            }
            endLatLng != null -> {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(endLatLng, 13.5f))
            }
            else -> {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(LatLng(35.681236, 139.767125), 11.5f)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        val next = (stretchOffset + dragAmount * 0.2f).coerceIn(-56f, 56f)
                        stretchOffset = next
                    },
                    onDragEnd = { stretchOffset = 0f },
                    onDragCancel = { stretchOffset = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer {
                    val offsetAbs = kotlin.math.abs(animatedStretch)
                    val stretchScale = 1f + (offsetAbs / 1800f)
                    scaleY = stretchScale
                    translationY = animatedStretch * 0.12f
                    transformOrigin = if (animatedStretch >= 0f) {
                        TransformOrigin(0.5f, 0f)
                    } else {
                        TransformOrigin(0.5f, 1f)
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 21.dp)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = strings.back, tint = textColor)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 21.dp)
                        .clickable { showActionMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = strings.editTrip, tint = textColor)
                }
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mapWeight),
                level = GlassLevel.SurfacePrimary,
                onClick = {}
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showMapContent) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = false),
                            uiSettings = MapUiSettings(
                                compassEnabled = false,
                                zoomControlsEnabled = false,
                                mapToolbarEnabled = false
                            )
                        ) {
                            if (startLatLng != null) {
                                Marker(
                                    state = startMarkerState!!,
                                    title = strings.start,
                                    snippet = inStationText,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                            if (endLatLng != null) {
                                Marker(
                                    state = endMarkerState!!,
                                    title = strings.end,
                                    snippet = outStationText,
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                            if (startLatLng != null && endLatLng != null) {
                                Polyline(
                                    points = listOf(startLatLng, endLatLng),
                                    color = lineColor,
                                    width = 12f,
                                    geodesic = true
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .glassSurface(level = GlassLevel.Overlay, cornerRadius = 12.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(24.dp)
                                    .background(lineColor, RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = lineCompany,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(detailsWeight),
                level = GlassLevel.SurfaceSecondary,
                onClick = {}
            ) {
                if (trip == null) {
                    Text(
                        text = strings.tripNotFound,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(trip.timestamp))
                    val typeText = TripUiFormatter.transactionTitle(trip.type, strings)
                    val inDisplay = trip.inStationName ?: trip.inStation
                    val outDisplay = trip.outStationName ?: trip.outStation

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = trip.customTitle ?: typeText,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = dateText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(text = "${strings.inPrefix} $inDisplay", color = Color.White.copy(alpha = 0.88f), fontSize = 16.sp)
                        Text(text = "${strings.outPrefix} $outDisplay", color = Color.White.copy(alpha = 0.88f), fontSize = 16.sp)
                        Text(
                            text = "${strings.amountLabel}: ${if (trip.amount > 0) "+" else ""}¥${trip.amount}",
                            color = if (trip.amount > 0) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${strings.balanceLabel}: ¥${trip.balance}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${strings.rawBlockLabel}: ${trip.blockHex}",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                level = GlassLevel.SurfaceSecondary,
                onClick = { showNoteDialog = true }
            ) {
                Text(
                    text = trip?.note?.takeIf { it.isNotBlank() } ?: strings.noNote,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    maxLines = if (noteLength > 180) 4 else 2
                )
            }
        }

        CompositionLocalProvider(LocalOverlayBackdrop provides appBackdrop) {
            if (showActionMenu) {
                GlassMenuDialog(
                    onDismissRequest = { showActionMenu = false },
                    onEditClick = {
                        showActionMenu = false
                        showEditDialog = true
                    },
                    onDeleteClick = {
                        showActionMenu = false
                        showDeleteConfirm = true
                    },
                    title = strings.tripOptionsTitle,
                    editText = strings.editTripOption,
                    deleteText = strings.deleteTripOption
                )
            }

            if (showDeleteConfirm && trip != null) {
                GlassConfirmDialog(
                    title = strings.deleteTripTitle,
                    message = strings.deleteTripMessage,
                    confirmText = strings.deleteCard,
                    confirmColor = Color(0xFFFF6B6B),
                    onDismissRequest = { showDeleteConfirm = false },
                    onConfirmClick = {
                        onDeleteTrip(trip)
                        showDeleteConfirm = false
                        onBackClick()
                    }
                )
            }

            if (showEditDialog && trip != null) {
                GlassOverlayContainer(onDismissRequest = { showEditDialog = false }) {
                    GlassDialogPanel(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .widthIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(strings.editTrip, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = editingTitle,
                                onValueChange = { editingTitle = it },
                                label = { Text(strings.tripNameLabel, color = textColor.copy(alpha = 0.72f)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDateFormat.format(Date(selectedEditDateMillis)),
                                onValueChange = {},
                                label = { Text(strings.dateLabel, color = textColor.copy(alpha = 0.72f)) },
                                placeholder = { Text(strings.dateInputHint) },
                                readOnly = true,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.35f),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePickerDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = strings.pickDate,
                                            tint = textColor
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePickerDialog = true }
                            )
                            OutlinedTextField(
                                value = editingAmount,
                                onValueChange = {
                                    editingAmount = it
                                    editValidationMessage = null
                                },
                                label = { Text(strings.amountLabel, color = textColor.copy(alpha = 0.72f)) },
                                placeholder = { Text(strings.amountInputHint) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            editValidationMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = Color(0xFFFF8A80),
                                    fontSize = 12.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showEditDialog = false }) {
                                    Text(strings.cancel, color = textColor.copy(alpha = 0.75f))
                                }
                                Button(
                                    onClick = {
                                        val parsedAmount = editingAmount.trim().toIntOrNull()
                                        if (parsedAmount == null) {
                                            editValidationMessage = strings.invalidAmountMessage
                                        } else {
                                            onSaveEdit(
                                                trip,
                                                editingTitle,
                                                trip.note ?: "",
                                                selectedEditDateMillis,
                                                parsedAmount
                                            )
                                            showEditDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.22f),
                                        contentColor = textColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(strings.save) }
                            }
                        }
                    }
                }
            }

            if (showNoteDialog && trip != null) {
                GlassOverlayContainer(onDismissRequest = { showNoteDialog = false }) {
                    GlassDialogPanel(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .widthIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(strings.noteLabel, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = editingNote,
                                onValueChange = { editingNote = it },
                                label = { Text(strings.noteLabel, color = textColor.copy(alpha = 0.72f)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showNoteDialog = false }) {
                                    Text(strings.cancel, color = textColor.copy(alpha = 0.75f))
                                }
                                Button(
                                    onClick = {
                                        onSaveEdit(
                                            trip,
                                            trip.customTitle ?: "",
                                            editingNote,
                                            trip.timestamp,
                                            trip.amount
                                        )
                                        showNoteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.22f),
                                        contentColor = textColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(strings.save) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedEditDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedEditDateMillis = it
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
