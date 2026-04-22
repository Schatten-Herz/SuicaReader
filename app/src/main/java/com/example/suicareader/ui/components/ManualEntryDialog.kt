package com.example.suicareader.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.suicareader.nfc.StationResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: Int, amount: Int, inStationCode: String, inStationName: String, outStationCode: String?, outStationName: String?) -> Unit
) {
    var isCharge by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    
    var inStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var outStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    var pickingFor by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"

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

    val strings = com.example.suicareader.ui.theme.LocalStrings.current
    val textColor = com.example.suicareader.ui.theme.LocalTextColor.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(strings.addManualEntry, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Type Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !isCharge,
                        onClick = { isCharge = false },
                        label = { Text(strings.typeFare) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isCharge,
                        onClick = { isCharge = true },
                        label = { Text(strings.typeCharge) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // In Station
                OutlinedButton(
                    onClick = { pickingFor = "IN" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(inStation?.second ?: strings.selectInStation, color = textColor)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Out Station
                if (!isCharge) {
                    OutlinedButton(
                        onClick = { pickingFor = "OUT" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(outStation?.second ?: strings.selectOutStation, color = textColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = textColor.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toIntOrNull() ?: 0
                            val type = if (isCharge) 0x02 else 0x01
                            val finalAmt = if (isCharge) amt else -amt
                            if (inStation != null) {
                                onSubmit(type, finalAmt, inStation!!.first, inStation!!.second,outStation?.first,outStation?.second)
                            }
                        },
                        enabled = amountStr.isNotBlank() && inStation != null && (isCharge || outStation != null)
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
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
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f)
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
