package com.example.suicareader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.suicareader.nfc.StationResolver
import com.example.suicareader.ui.theme.LocalStrings
import com.example.suicareader.ui.theme.LocalTextColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        type: Int,
        amount: Int,
        inStationCode: String,
        inStationName: String,
        outStationCode: String?,
        outStationName: String?,
        timestamp: Long,
        customTitle: String?,
        note: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(EntryType.Train) }
    var amountStr by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var inStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var outStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busCompany by remember { mutableStateOf("") }

    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }

    var pickingFor by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"
    var pendingStationName by remember { mutableStateOf<String?>(null) }
    var pendingStationFor by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"
    var showBusPicker by remember { mutableStateOf(false) }

    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    Box(modifier = Modifier.fillMaxSize().zIndex(50f)) {
        GlassOverlayContainer(onDismissRequest = onDismiss) {
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
                            .padding(24.dp)
                    ) {
                        Text(
                            strings.addManualEntry,
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.dateLabel, color = textColor.copy(alpha = 0.7f))
                                Text(dateFormat.format(Date(timestamp)), color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                .clickable { showTypePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.typeLabel, color = textColor.copy(alpha = 0.7f))
                                Text(
                                    text = when (selectedType) {
                                        EntryType.Train -> strings.typeTrain
                                        EntryType.Bus -> strings.typeBus
                                        EntryType.Recharge -> strings.typeRecharge
                                        EntryType.Locker -> strings.typeLocker
                                        EntryType.Expense -> strings.typeExpense
                                    },
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text(strings.amountInput, color = textColor.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.3f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedType) {
                            EntryType.Train -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                        .clickable { pickingFor = "IN" }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.inShortLabel, color = textColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            inStation?.second ?: strings.selectInStation,
                                            color = if (inStation == null) textColor.copy(alpha = 0.6f) else textColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                        .clickable { pickingFor = "OUT" }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.outShortLabel, color = textColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            outStation?.second ?: strings.selectOutStation,
                                            color = if (outStation == null) textColor.copy(alpha = 0.6f) else textColor
                                        )
                                    }
                                }
                            }
                            EntryType.Bus -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                        .clickable { showBusPicker = true }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.busShortLabel, color = textColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (busCompany.isNotBlank()) busCompany else strings.selectBusCompany,
                                            color = if (busCompany.isBlank()) textColor.copy(alpha = 0.6f) else textColor
                                        )
                                    }
                                }
                            }
                            EntryType.Recharge -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(level = GlassLevel.DialogItem, cornerRadius = 14.dp)
                                        .clickable { pickingFor = "IN" }
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.locationShortLabel, color = textColor.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            inStation?.second ?: strings.selectRechargeStation,
                                            color = if (inStation == null) textColor.copy(alpha = 0.6f) else textColor
                                        )
                                    }
                                }
                            }
                            EntryType.Locker, EntryType.Expense -> {
                                OutlinedTextField(
                                    value = customTitle,
                                    onValueChange = { customTitle = it },
                                    label = { Text(strings.tripNameLabel, color = textColor.copy(alpha = 0.7f)) },
                                    colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(strings.noteLabel, color = textColor.copy(alpha = 0.7f)) },
                            colors = glassOutlinedTextFieldColors(textColor, unfocusedBorderAlpha = 0.3f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(strings.cancel, color = textColor.copy(alpha = 0.75f), fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val amt = amountStr.toIntOrNull() ?: 0
                                    when (selectedType) {
                                        EntryType.Train -> {
                                            if (inStation != null && outStation != null) {
                                                onSubmit(0x01, -amt, inStation!!.first, inStation!!.second, outStation!!.first, outStation!!.second, timestamp, customTitle, note)
                                            }
                                        }
                                        EntryType.Bus -> {
                                            onSubmit(0x0F, -amt, "BUS-00-00", busCompany, null, null, timestamp, customTitle, note)
                                        }
                                        EntryType.Recharge -> {
                                            if (inStation != null) {
                                                onSubmit(0x02, amt, inStation!!.first, inStation!!.second, null, null, timestamp, customTitle, note)
                                            }
                                        }
                                        EntryType.Locker -> {
                                            onSubmit(0x50, -amt, "LOCKER", strings.typeLocker, null, null, timestamp, customTitle.ifBlank { null }, note)
                                        }
                                        EntryType.Expense -> {
                                            onSubmit(0x46, -amt, "EXPENSE", strings.typeExpense, null, null, timestamp, customTitle.ifBlank { null }, note)
                                        }
                                    }
                                },
                                enabled = amountStr.isNotBlank() && when (selectedType) {
                                    EntryType.Train -> inStation != null && outStation != null
                                    EntryType.Bus -> busCompany.isNotBlank()
                                    EntryType.Recharge -> inStation != null
                                    EntryType.Locker, EntryType.Expense -> true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E).copy(alpha = 0.85f),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                                    disabledContentColor = Color.White.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(strings.save, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        if (pickingFor != null) {
                StationPickerDialog(
                    onDismiss = { pickingFor = null },
                    onStationSelected = { stationName ->
                        pendingStationName = stationName
                        pendingStationFor = pickingFor
                        pickingFor = null
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
                        val stationPair = if (companyOption == null) {
                            stationName to stationName
                        } else {
                            companyOption.stationCode to "$stationName (${companyOption.companyShortName})"
                        }
                        if (pendingStationFor == "IN") inStation = stationPair else outStation = stationPair
                        pendingStationName = null
                        pendingStationFor = null
                    }
                )
            }

            if (showBusPicker) {
                BusCompanyPickerDialog(
                    onDismiss = { showBusPicker = false },
                    onCompanySelected = { company ->
                        busCompany = company
                        showBusPicker = false
                    }
                )
            }

            if (showTypePicker) {
                GlassOverlayContainer(onDismissRequest = { showTypePicker = false }) {
                    GlassDialogPanel(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .widthIn(max = 420.dp),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(strings.chooseType, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            listOf(EntryType.Train, EntryType.Bus, EntryType.Recharge, EntryType.Locker, EntryType.Expense).forEach { type ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassSurface(
                                            level = GlassLevel.DialogItem,
                                            cornerRadius = 14.dp,
                                            isPressed = selectedType == type
                                        )
                                        .clickable {
                                            selectedType = type
                                            showTypePicker = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 13.dp)
                                ) {
                                    Text(
                                        text = when (type) {
                                            EntryType.Train -> strings.typeTrain
                                            EntryType.Bus -> strings.typeBus
                                            EntryType.Recharge -> strings.typeRecharge
                                            EntryType.Locker -> strings.typeLocker
                                            EntryType.Expense -> strings.typeExpense
                                        },
                                        color = textColor,
                                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp = it }
                    showDatePicker = false
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

enum class EntryType {
    Train, Bus, Recharge, Locker, Expense
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerDialog(
    onDismiss: () -> Unit,
    onStationSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { StationResolver.searchStationNames(query) }
    val popularStations = remember { StationResolver.popularStationNames() }
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    GlassOverlayContainer(onDismissRequest = onDismiss) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.78f)
                .widthIn(max = 540.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Text(strings.searchStation, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchPlaceholder, color = textColor.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.45f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        if (popularStations.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.popularStations,
                                    color = textColor.copy(alpha = 0.72f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                            }
                            items(popularStations.size) { i ->
                                val stationName = popularStations[i]
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStationSelected(stationName) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                ) {
                                    Text(stationName, color = textColor, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                            }
                        } else {
                            item {
                                Text(
                                    text = strings.searchPlaceholder,
                                    color = textColor.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(results.size) { i ->
                            val stationName = results[i]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationSelected(stationName) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(stationName, color = textColor, fontWeight = FontWeight.Medium)
                            }
                            HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                        }
                    }
                    if (query.isNotBlank() && results.isEmpty()) {
                        item {
                            Text(strings.noResults, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationCompanyPickerDialog(
    stationName: String,
    onDismiss: () -> Unit,
    onCompanySelected: (StationResolver.StationCompanyOption?) -> Unit
) {
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current
    val companies = remember(stationName) { StationResolver.companyOptionsForStation(stationName) }

    GlassOverlayContainer(onDismissRequest = onDismiss) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 460.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(
                    text = stationName,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.selectRailCompany,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompanySelected(null) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(strings.noneLabel, color = textColor, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    }
                    items(companies.size) { i ->
                        val company = companies[i]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompanySelected(company) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(company.companyShortName, color = textColor, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusCompanyPickerDialog(
    onDismiss: () -> Unit,
    onCompanySelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { StationResolver.searchBusCompanies(query) }
    val strings = LocalStrings.current
    val textColor = LocalTextColor.current

    GlassOverlayContainer(onDismissRequest = onDismiss) {
        GlassDialogPanel(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.78f)
                .widthIn(max = 540.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Text(strings.selectBusCompanyTitle, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchPlaceholder, color = textColor.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.45f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results.size) { i ->
                        val company = results[i]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompanySelected(company) }
                                .padding(vertical = 15.dp, horizontal = 4.dp)
                        ) {
                            Text(company, color = textColor)
                        }
                        HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    }
                    if (results.isEmpty()) {
                        item {
                            Text(strings.noResults, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
