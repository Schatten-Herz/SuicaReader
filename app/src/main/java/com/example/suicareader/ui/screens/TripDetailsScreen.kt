package com.example.suicareader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.map.TransitMapCatalog
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import com.example.suicareader.ui.components.GlassCard
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripDetailsScreen(
    trip: TripRecord?,
    onSaveEdit: (TripRecord, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingTitle by remember(trip?.id, showEditDialog) { mutableStateOf(trip?.customTitle ?: "") }
    var editingNote by remember(trip?.id, showEditDialog, showNoteDialog) { mutableStateOf(trip?.note ?: "") }
    val isSubLayerOpen = showEditDialog || showNoteDialog
    val inStationText = trip?.inStationName ?: trip?.inStation
    val outStationText = trip?.outStationName ?: trip?.outStation
    val startLatLng = remember(inStationText) { TransitMapCatalog.coordinateForStation(inStationText) }
    val endLatLng = remember(outStationText) { TransitMapCatalog.coordinateForStation(outStationText) }
    val lineCompany = remember(inStationText) { TransitMapCatalog.companyName(inStationText) }
    val lineColor = remember(lineCompany) { TransitMapCatalog.colorForCompany(lineCompany) }
    val cameraPositionState = rememberCameraPositionState()
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
    LaunchedEffect(startLatLng, endLatLng) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isSubLayerOpen) 14.dp else 0.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = strings.editTrip, tint = textColor)
                }
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mapWeight),
                onClick = {}
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                                state = MarkerState(startLatLng),
                                title = "Start",
                                snippet = inStationText,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                        if (endLatLng != null) {
                            Marker(
                                state = MarkerState(endLatLng),
                                title = "End",
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    if (startLatLng == null || endLatLng == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.mapPreviewComingSoon,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 14.sp
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
                    val typeText = when (trip.type) {
                        0x01 -> strings.fareSubway
                        0x02 -> strings.chargeTopUp
                        0x0F, 0x0D -> strings.busFare
                        0x46 -> strings.purchase
                        0x50 -> strings.typeLocker
                        else -> "Transaction (0x${"%02X".format(trip.type)})"
                    }
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
                        Text(text = "${strings.inPrefix} $inDisplay", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                        Text(text = "${strings.outPrefix} $outDisplay", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
                        Text(
                            text = "Amount: ${if (trip.amount > 0) "+" else ""}¥${trip.amount}",
                            color = if (trip.amount > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${strings.balanceLabel}: ¥${trip.balance}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Raw Block: ${trip.blockHex}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showNoteDialog = true }
            ) {
                Text(
                    text = trip?.note?.takeIf { it.isNotBlank() } ?: strings.noNote,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = if (noteLength > 180) 4 else 2
                )
            }
        }
    }

    if (showEditDialog && trip != null) {
        Dialog(onDismissRequest = { showEditDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .glassSurface(cornerRadius = 24.dp, fillAlpha = 0.20f)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(strings.editTrip, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editingTitle,
                        onValueChange = { editingTitle = it },
                        label = { Text(strings.tripNameLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditDialog = false }) { Text(strings.cancel) }
                        Button(
                            onClick = {
                                onSaveEdit(trip, editingTitle, trip.note ?: "")
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = textColor
                            )
                        ) { Text(strings.save) }
                    }
                }
            }
        }
    }

    if (showNoteDialog && trip != null) {
        Dialog(onDismissRequest = { showNoteDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .glassSurface(cornerRadius = 24.dp, fillAlpha = 0.20f)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(strings.noteLabel, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editingNote,
                        onValueChange = { editingNote = it },
                        label = { Text(strings.noteLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showNoteDialog = false }) { Text(strings.cancel) }
                        Button(
                            onClick = {
                                onSaveEdit(trip, trip.customTitle ?: "", editingNote)
                                showNoteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = textColor
                            )
                        ) { Text(strings.save) }
                    }
                }
            }
        }
    }
}
