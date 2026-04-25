package com.example.suicareader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.suicareader.nfc.StationResolver
import com.example.suicareader.ui.components.glassSurface
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
    var showBusPicker by remember { mutableStateOf(false) }
    val isSubLayerOpen = pickingFor != null || showBusPicker || showTypePicker || showDatePicker

    val strings = com.example.suicareader.ui.theme.LocalStrings.current
    val textColor = com.example.suicareader.ui.theme.LocalTextColor.current

    if (pickingFor != null) {
        StationPickerDialog(
            onDismiss = { pickingFor = null },
            onStationSelected = { station ->
                if (pickingFor == "IN") inStation = station else outStation = station
                pickingFor = null
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
        Dialog(
            onDismissRequest = { showTypePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .glassSurface(cornerRadius = 20.dp, fillAlpha = 0.22f)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.chooseType, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    listOf(EntryType.Train, EntryType.Bus, EntryType.Recharge, EntryType.Locker, EntryType.Expense).forEach { type ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = type
                                    showTypePicker = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedType == type) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
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
                                fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Main container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // High blur layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(50.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(if (isSubLayerOpen) 14.dp else 0.dp)
                ) {
                    Column {
                Text(strings.addManualEntry, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(24.dp))

                // Date only
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface(cornerRadius = 12.dp, fillAlpha = 0.14f, borderAlphaStrong = 0.35f, borderAlphaWeak = 0.08f)
                        .clickable { showDatePicker = true },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.dateLabel, color = textColor.copy(alpha = 0.7f))
                        Text(dateFormat.format(Date(timestamp)), color = textColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Type selector (menu style, easier to scale with more types)
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSurface(cornerRadius = 12.dp, fillAlpha = 0.14f, borderAlphaStrong = 0.35f, borderAlphaWeak = 0.08f)
                            .clickable { showTypePicker = true },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(strings.amountInput, color = textColor.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Dynamic Fields based on Type
                when (selectedType) {
                    EntryType.Train -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { pickingFor = "IN" },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("In: ", color = textColor.copy(alpha = 0.5f))
                                Text(inStation?.second ?: strings.selectInStation, color = if (inStation == null) textColor.copy(alpha = 0.5f) else textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { pickingFor = "OUT" },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Out: ", color = textColor.copy(alpha = 0.5f))
                                Text(outStation?.second ?: strings.selectOutStation, color = if (outStation == null) textColor.copy(alpha = 0.5f) else textColor)
                            }
                        }
                    }
                    EntryType.Bus -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showBusPicker = true },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Bus: ", color = textColor.copy(alpha = 0.5f))
                                Text(if (busCompany.isNotBlank()) busCompany else "Select Bus Company", color = if (busCompany.isBlank()) textColor.copy(alpha = 0.5f) else textColor)
                            }
                        }
                    }
                    EntryType.Recharge -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { pickingFor = "IN" },
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Location: ", color = textColor.copy(alpha = 0.5f))
                                Text(inStation?.second ?: "Select Recharge Station", color = if (inStation == null) textColor.copy(alpha = 0.5f) else textColor)
                            }
                        }
                    }
                    EntryType.Locker, EntryType.Expense -> {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text(strings.tripNameLabel, color = textColor.copy(alpha = 0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = textColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedContainerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel, color = textColor.copy(alpha = 0.7f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(strings.cancel, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
                                    onSubmit(0x50, -amt, "LOCKER", "Locker", null, null, timestamp, customTitle.ifBlank { "Locker" }, note)
                                }
                                EntryType.Expense -> {
                                    onSubmit(0x46, -amt, "EXPENSE", "Expense", null, null, timestamp, customTitle.ifBlank { "Expense" }, note)
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
                            containerColor = Color(0xFF4CAF50), // Using a distinct accent color for primary action
                            contentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.1f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).padding(horizontal = 16.dp)
                    ) {
                        Text(strings.save, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                    }
                }
            }
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
    onStationSelected: (Pair<String, String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf<String?>(null) }
    
    val companyFilters = remember {
        listOf(
            "東日本旅客鉄道",
            "東京メトロ",
            "東京都交通局",
            "京急電鉄",
            "京成電鉄",
            "東急電鉄"
        )
    }

    val results = remember(query, selectedCompany) { StationResolver.searchStations(query, selectedCompany) }
    val strings = com.example.suicareader.ui.theme.LocalStrings.current
    val textColor = com.example.suicareader.ui.theme.LocalTextColor.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(24.dp))
                    .glassSurface(cornerRadius = 24.dp, fillAlpha = 0.18f)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(50.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(strings.searchStation, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchPlaceholder, color = textColor.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCompany == null,
                        onClick = { selectedCompany = null },
                        label = { Text("All") }
                    )
                    companyFilters.forEach { company ->
                        FilterChip(
                            selected = selectedCompany == company,
                            onClick = {
                                selectedCompany = if (selectedCompany == company) null else company
                            },
                            label = { Text(company, maxLines = 1) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        item {
                            Text(
                                text = strings.searchPlaceholder,
                                color = textColor.copy(alpha = 0.55f),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(results.size) { i ->
                            val station = results[i]
                            val stationName = station.second.substringBefore(" (")
                            val company = station.second.substringAfter("(", "").substringBefore(")")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationSelected(station) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Column {
                                    Text(stationName, color = textColor, fontWeight = FontWeight.Medium)
                                    if (company.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(company, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusCompanyPickerDialog(
    onDismiss: () -> Unit,
    onCompanySelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val results = remember(query) { StationResolver.searchBusCompanies(query) }
    
    val strings = com.example.suicareader.ui.theme.LocalStrings.current
    val textColor = com.example.suicareader.ui.theme.LocalTextColor.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(24.dp))
                    .glassSurface(cornerRadius = 24.dp, fillAlpha = 0.18f)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(50.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Select Bus Company", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchPlaceholder, color = textColor.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(results.size) { i ->
                        val company = results[i]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompanySelected(company) }
                                .padding(vertical = 16.dp)
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
}
