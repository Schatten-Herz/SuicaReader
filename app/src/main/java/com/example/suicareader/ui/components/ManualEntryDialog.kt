package com.example.suicareader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: Int, amount: Int, inStationCode: String, inStationName: String, outStationCode: String?, outStationName: String?, timestamp: Long) -> Unit
) {
    var selectedType by remember { mutableStateOf(EntryType.Train) } // Train, Bus, Recharge
    var amountStr by remember { mutableStateOf("") }
    
    var inStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var outStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busCompany by remember { mutableStateOf("") }
    
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var pickingFor by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"
    var showBusPicker by remember { mutableStateOf(false) }

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
        return
    }

    if (showBusPicker) {
        BusCompanyPickerDialog(
            onDismiss = { showBusPicker = false },
            onCompanySelected = { company ->
                busCompany = company
                showBusPicker = false
            }
        )
        return
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
                Text(strings.addManualEntry, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(24.dp))

                // Date Picker Button (More distinct)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", color = textColor.copy(alpha = 0.7f))
                        Text(dateFormat.format(Date(timestamp)), color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Type Toggle (Segmented control style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(EntryType.Train, EntryType.Bus, EntryType.Recharge).forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(type) {
                                    EntryType.Train -> "Train"
                                    EntryType.Bus -> "Bus"
                                    EntryType.Recharge -> "Recharge"
                                },
                                color = if (isSelected) textColor else textColor.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
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
                                        onSubmit(0x01, -amt, inStation!!.first, inStation!!.second, outStation!!.first, outStation!!.second, timestamp)
                                    }
                                }
                                EntryType.Bus -> {
                                    onSubmit(0x0F, -amt, "BUS-00-00", busCompany, null, null, timestamp)
                                }
                                EntryType.Recharge -> {
                                    if (inStation != null) {
                                        onSubmit(0x02, amt, inStation!!.first, inStation!!.second, null, null, timestamp)
                                    }
                                }
                            }
                        },
                        enabled = amountStr.isNotBlank() && when (selectedType) {
                            EntryType.Train -> inStation != null && outStation != null
                            EntryType.Bus -> busCompany.isNotBlank()
                            EntryType.Recharge -> inStation != null
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

enum class EntryType {
    Train, Bus, Recharge
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerDialog(
    onDismiss: () -> Unit,
    onStationSelected: (Pair<String, String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val results = remember(query) { StationResolver.searchStations(query, null) }
    val groupedResults = remember(results) { 
        results.groupBy { it.second.substringAfter("(", "").substringBefore(")") } 
    }
    
    val strings = com.example.suicareader.ui.theme.LocalStrings.current
    val textColor = com.example.suicareader.ui.theme.LocalTextColor.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(50.dp)
                    .background(Color.White.copy(alpha = 0.1f))
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    groupedResults.forEach { (company, stations) ->
                        val groupName = company.takeIf { it.isNotBlank() } ?: "Other"
                        item {
                            Text(
                                text = groupName,
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                            Divider(color = textColor.copy(alpha = 0.1f))
                        }
                        items(stations.size) { i ->
                            val station = stations[i]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationSelected(station) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(station.second, color = textColor)
                            }
                            Divider(color = textColor.copy(alpha = 0.1f))
                        }
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(50.dp)
                    .background(Color.White.copy(alpha = 0.1f))
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
