package com.example.suicareader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
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
import com.example.suicareader.ui.components.StationCompanyPickerDialog
import com.example.suicareader.ui.components.StationPickerDialog
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
    onSaveEdit: (
        TripRecord,
        String,
        String,
        Long,
        Int,
        String,
        String?,
        String,
        String?
    ) -> Unit,
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
    var editingInStationCode by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.inStation ?: "") }
    var editingInStationName by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.inStationName) }
    var editingOutStationCode by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.outStation ?: "") }
    var editingOutStationName by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.outStationName) }
    var pickingStationFor by remember { mutableStateOf<String?>(null) }
    var pendingStationName by remember { mutableStateOf<String?>(null) }
    var pendingStationFor by remember { mutableStateOf<String?>(null) }
    var isMapTouched by remember { mutableStateOf(false) }

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
    val noteLength = trip?.note?.length ?: 0

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapCardHeight = (maxHeight * 0.40f).coerceIn(240.dp, 360.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .verticalScroll(rememberScrollState(), enabled = !isMapTouched)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    .height(mapCardHeight),
                level = GlassLevel.SurfacePrimary,
                onClick = {}
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                isMapTouched = true
                                try {
                                    do {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    isMapTouched = false
                                }
                            }
                        }
                ) {
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
                                key(startLatLng) {
                                    Marker(
                                        state = startMarkerState!!,
                                        title = strings.start,
                                        snippet = inStationText,
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                    )
                                }
                            }
                            if (endLatLng != null) {
                                key(endLatLng) {
                                    Marker(
                                        state = endMarkerState!!,
                                        title = strings.end,
                                        snippet = outStationText,
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                    )
                                }
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
                modifier = Modifier.fillMaxWidth(),
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
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                            .heightIn(max = 680.dp)
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
                            val inDisplayValue = editingInStationName?.takeIf { it.isNotBlank() }
                                ?: editingInStationCode.takeIf { it.isNotBlank() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                    .clickable { pickingStationFor = "IN" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (trip.type == 0x01) strings.inShortLabel else strings.locationShortLabel,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = inDisplayValue ?: strings.selectInStation,
                                        color = if (inDisplayValue == null) textColor.copy(alpha = 0.6f) else textColor
                                    )
                                }
                            }
                            if (trip.type == 0x01 || !editingOutStationName.isNullOrBlank() || editingOutStationCode.isNotBlank()) {
                                val outDisplayValue = editingOutStationName?.takeIf { it.isNotBlank() }
                                    ?: editingOutStationCode.takeIf { it.isNotBlank() }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                        .clickable { pickingStationFor = "OUT" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(strings.outShortLabel, color = textColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = outDisplayValue ?: strings.selectOutStation,
                                            color = if (outDisplayValue == null) textColor.copy(alpha = 0.6f) else textColor
                                        )
                                    }
                                }
                            }
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
                                                parsedAmount,
                                                editingInStationCode,
                                                editingInStationName,
                                                editingOutStationCode,
                                                editingOutStationName
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
                                            trip.amount,
                                            trip.inStation,
                                            trip.inStationName,
                                            trip.outStation,
                                            trip.outStationName
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

            if (pickingStationFor != null) {
                StationPickerDialog(
                    onDismiss = { pickingStationFor = null },
                    onStationSelected = { stationName ->
                        pendingStationName = stationName
                        pendingStationFor = pickingStationFor
                        pickingStationFor = null
                    }
                )
            }

            if (pendingStationName != null && pendingStationFor != null) {
                StationCompanyPickerDialog(
                    stationName = pendingStationName!!,
                    onDismiss = {
                        pendingStationName = null
                        pendingStationFor = null
                    },
                    onCompanySelected = { companyOption ->
                        val stationName = pendingStationName!!
                        val code = companyOption?.stationCode ?: stationName
                        val displayName = if (companyOption == null) {
                            stationName
                        } else {
                            "$stationName (${companyOption.companyShortName})"
                        }
                        if (pendingStationFor == "IN") {
                            editingInStationCode = code
                            editingInStationName = displayName
                        } else {
                            editingOutStationCode = code
                            editingOutStationName = displayName
                        }
                        pendingStationName = null
                        pendingStationFor = null
                    }
                )
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
