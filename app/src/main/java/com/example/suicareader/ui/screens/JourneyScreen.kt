package com.example.suicareader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.components.GlassCard
import com.example.suicareader.ui.components.GlassLevel
import com.example.suicareader.ui.components.glassSurface
import com.example.suicareader.ui.map.TransitMapCatalog
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.FilterList
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.pow

private const val CityAll = "__all__"
private const val CityTokyo = "tokyo"
private const val CityOsaka = "osaka"
private const val CityKyoto = "kyoto"
private const val CityYokohama = "yokohama"
private const val CityNagoya = "nagoya"
private const val CityFukuoka = "fukuoka"
private const val CityOther = "other"
private const val AnalysisCompany = "company"
private const val AnalysisCity = "city"

@Composable
fun JourneyScreen(viewModel: MainViewModel) {
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val cards by viewModel.cards.collectAsState()
    val selectedCardIds = remember(cards) { mutableStateListOf<String>().apply { addAll(cards.map { it.idm }) } }
    val trips by viewModel.getTripsForCards(selectedCardIds.toList()).collectAsState(initial = emptyList())
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(36.2048, 138.2529), 4.8f)
    }
    var showCardDropdown by rememberSaveable { mutableStateOf(false) }
    var showCityDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedCity by rememberSaveable { mutableStateOf(CityAll) }
    var analysisMode by rememberSaveable { mutableStateOf(AnalysisCompany) }

    val cityStats = remember(trips) {
        viewModel.cityStats(trips) { trip ->
            val inCity = cityFromLatLng(TransitMapCatalog.coordinateForStation(trip.inStationName ?: trip.inStation))
            val outCity = cityFromLatLng(TransitMapCatalog.coordinateForStation(trip.outStationName ?: trip.outStation))
            inCity ?: outCity
        }
    }
    val cityOptions = remember(cityStats, strings) {
        listOf(CityAll to strings.journeyNationwide) + cityStats.map {
            it.city to "${cityDisplayLabel(it.city, strings)} (${it.count})"
        }
    }
    val mappedTrips = remember(trips, selectedCity) { mapTripsForCity(trips, selectedCity) }
    val topCompanies = remember(trips) { viewModel.topCompanyStats(trips, topN = 3) }
    val topCities = remember(cityStats, strings) {
        cityStats.take(3).map { MainViewModel.JourneyStatItem(cityDisplayLabel(it.city, strings), it.count) }
    }
    val companyCount = remember(trips) { viewModel.topCompanyStats(trips, topN = Int.MAX_VALUE).size }
    val cityCount = remember(cityStats) { cityStats.count { it.city != CityOther } }
    val activeStats = if (analysisMode == AnalysisCompany) topCompanies else topCities
    val selectedCityLabel = remember(selectedCity, strings) {
        if (selectedCity == CityAll) strings.journeyNationwide else cityDisplayLabel(selectedCity, strings)
    }

    LaunchedEffect(Unit) {
        TransitMapCatalog.init(context)
    }

    LaunchedEffect(selectedCity) {
        if (selectedCity == CityAll) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(36.2048, 138.2529), 4.8f),
                1200
            )
        }
    }

    LaunchedEffect(selectedCity, mappedTrips) {
        if (selectedCity == CityAll) return@LaunchedEffect
        val points = mappedTrips.flatMap { listOfNotNull(it.start, it.end) }
        if (points.isEmpty()) return@LaunchedEffect
        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120),
            1200
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(strings.journeyTitle, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val chipGap = 10.dp
                val halfChipColumnWidth = (maxWidth - chipGap) / 2
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(chipGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            JourneyFilterChip(
                                text = strings.journeyFilterCards,
                                expanded = showCardDropdown,
                                onClick = {
                                    showCardDropdown = !showCardDropdown
                                    if (showCardDropdown) showCityDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            JourneyFilterChip(
                                text = selectedCityLabel,
                                expanded = showCityDropdown,
                                onClick = {
                                    showCityDropdown = !showCityDropdown
                                    if (showCityDropdown) showCardDropdown = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxSize(),
                        level = GlassLevel.SurfacePrimary,
                        onClick = {}
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(isMyLocationEnabled = false),
                                uiSettings = MapUiSettings(
                                    compassEnabled = false,
                                    zoomControlsEnabled = false,
                                    mapToolbarEnabled = false
                                )
                            ) {
                                val zoom = cameraPositionState.position.zoom.toDouble()
                                val zoomScale = (2.0.pow(11.0 - zoom)).coerceIn(0.7, 8.0)
                                val nationwideOuterRadius = 5200.0 * zoomScale
                                val nationwideCoreRadius = 2500.0 * zoomScale
                                if (selectedCity == CityAll) {
                                    mappedTrips
                                        .flatMap { listOfNotNull(it.start, it.end) }
                                        .forEach { point ->
                                            Circle(
                                                center = point,
                                                radius = nationwideOuterRadius,
                                                fillColor = Color(0x40FF7A59),
                                                strokeColor = Color(0x00FFFFFF),
                                                strokeWidth = 0f
                                            )
                                            Circle(
                                                center = point,
                                                radius = nationwideCoreRadius,
                                                fillColor = Color(0x80FF7A59),
                                                strokeColor = Color(0x00FFFFFF),
                                                strokeWidth = 0f
                                            )
                                        }
                                } else {
                                    mappedTrips.forEach { item ->
                                        if (item.start != null && item.end != null) {
                                            Polyline(
                                                points = listOf(item.start, item.end),
                                                color = item.color,
                                                geodesic = true,
                                                width = 8f
                                            )
                                            Circle(
                                                center = item.start,
                                                radius = 140.0,
                                                fillColor = Color(0x8C4FA3FF),
                                                strokeColor = Color(0x00FFFFFF),
                                                strokeWidth = 0f
                                            )
                                            Circle(
                                                center = item.end,
                                                radius = 140.0,
                                                fillColor = Color(0x8CFF6F61),
                                                strokeColor = Color(0x00FFFFFF),
                                                strokeWidth = 0f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingDropdownMenu(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 52.dp)
                        .width(halfChipColumnWidth)
                        .zIndex(2f),
                    visible = showCardDropdown
                ) {
                    cards.forEach { card ->
                        GlassCheckboxRow(
                            label = card.nickname,
                            checked = selectedCardIds.contains(card.idm),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!selectedCardIds.contains(card.idm)) selectedCardIds.add(card.idm)
                                } else if (selectedCardIds.size > 1) {
                                    selectedCardIds.remove(card.idm)
                                }
                            }
                        )
                    }
                }

                FloatingDropdownMenu(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 52.dp)
                        .offset(x = halfChipColumnWidth + chipGap)
                        .width(halfChipColumnWidth)
                        .zIndex(2f),
                    visible = showCityDropdown
                ) {
                    cityOptions.forEach { option ->
                        GlassCheckboxRow(
                            label = option.second,
                            checked = selectedCity == option.first,
                            onCheckedChange = {
                                selectedCity = option.first
                                showCityDropdown = false
                            },
                            singleSelect = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JourneyCountCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { analysisMode = AnalysisCompany },
                    label = strings.journeyModeCompanies,
                    value = companyCount.toString(),
                    active = analysisMode == AnalysisCompany,
                    icon = Icons.Default.DirectionsRailway,
                    isCompanyCard = true
                )
                JourneyCountCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { analysisMode = AnalysisCity },
                    label = strings.journeyTopCardSubtitle,
                    value = cityCount.toString(),
                    active = analysisMode == AnalysisCity,
                    icon = Icons.Default.Apartment,
                    isCompanyCard = false
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            JourneyStatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                title = strings.journeyTopCardTitle,
                subtitle = if (analysisMode == AnalysisCompany) strings.journeyModeCompanies else strings.journeyTopCardSubtitle,
                items = activeStats,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun JourneyFilterChip(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable (() -> Unit))? = null
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "journeyFilterChipArrow"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .glassSurface(level = GlassLevel.Overlay, cornerRadius = 18.dp)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (expanded) 1f else 0.8f),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = arrowRotation }
        )
    }
}

@Composable
private fun FloatingDropdownMenu(
    modifier: Modifier = Modifier,
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            expandVertically(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ),
        exit = fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)) +
            shrinkVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top
            ),
        modifier = modifier
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(22.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .glassSurface(level = GlassLevel.Overlay, cornerRadius = 16.dp)
                    .background(Color(0xAA534856))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                    .heightIn(max = 220.dp)
                    .padding(vertical = 6.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item { content() }
                }
            }
        }
    }
}

@Composable
private fun GlassCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    singleSelect: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun JourneyCountCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    active: Boolean,
    icon: ImageVector,
    isCompanyCard: Boolean
) {
    val iconAlpha by animateFloatAsState(
        targetValue = if (active) 0.96f else 0.76f,
        label = "journeyCountCardIconAlpha"
    )
    val unitSuffix = if (isCompanyCard) "家" else "个"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .glassSurface(level = GlassLevel.SurfaceSecondary, cornerRadius = 20.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = if (active) 0.14f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = iconAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = if (active) 0.96f else 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = unitSuffix,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyStatCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    items: List<MainViewModel.JourneyStatItem>,
    textColor: Color
) {
    GlassCard(modifier = modifier, level = GlassLevel.SurfaceSecondary, onClick = {}) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = textColor.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            )
            Spacer(modifier = Modifier.height(1.dp))
            if (items.isEmpty()) {
                Text("-", color = textColor.copy(alpha = 0.7f))
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${item.label}",
                                color = textColor,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.count.toString(),
                                color = textColor.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class JourneyMapTrip(
    val start: LatLng?,
    val end: LatLng?,
    val color: Color
)

private fun mapTripsForCity(trips: List<TripRecord>, selectedCity: String): List<JourneyMapTrip> {
    return trips.map { trip ->
        val startName = trip.inStationName ?: trip.inStation
        val endName = trip.outStationName ?: trip.outStation
        val start = TransitMapCatalog.coordinateForStation(startName)
        val end = TransitMapCatalog.coordinateForStation(endName)
        val cityMatches = selectedCity == CityAll ||
            cityFromLatLng(start) == selectedCity ||
            cityFromLatLng(end) == selectedCity
        if (!cityMatches) {
            JourneyMapTrip(null, null, Color(0xFF7E57C2))
        } else {
            JourneyMapTrip(
                start = start,
                end = end,
                color = TransitMapCatalog.colorForCompany(TransitMapCatalog.companyName(startName))
            )
        }
    }.filter { it.start != null || it.end != null }
}

private fun cityFromLatLng(latLng: LatLng?): String? {
    if (latLng == null) return null
    val lat = latLng.latitude
    val lng = latLng.longitude
    return when {
        lat in 35.45..35.9 && lng in 139.45..139.95 -> CityTokyo
        lat in 34.55..34.85 && lng in 135.35..135.7 -> CityOsaka
        lat in 35.0..35.15 && lng in 135.65..135.9 -> CityKyoto
        lat in 35.35..35.55 && lng in 139.55..139.75 -> CityYokohama
        lat in 35.05..35.3 && lng in 136.75..137.05 -> CityNagoya
        lat in 33.45..33.7 && lng in 130.2..130.55 -> CityFukuoka
        else -> CityOther
    }
}

private fun cityDisplayLabel(city: String, strings: com.example.suicareader.ui.theme.AppStrings): String {
    return when (city) {
        CityTokyo -> strings.cityTokyo
        CityOsaka -> strings.cityOsaka
        CityKyoto -> strings.cityKyoto
        CityYokohama -> strings.cityYokohama
        CityNagoya -> strings.cityNagoya
        CityFukuoka -> strings.cityFukuoka
        else -> strings.cityOther
    }
}
