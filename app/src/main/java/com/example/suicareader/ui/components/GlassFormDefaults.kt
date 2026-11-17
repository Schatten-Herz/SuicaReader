package com.example.suicareader.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.suicareader.data.db.entity.TripRecord
import com.example.suicareader.ui.theme.AppStrings

@Composable
fun glassOutlinedTextFieldColors(
    textColor: Color = Color.White,
    unfocusedBorderAlpha: Float = 0.32f
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedBorderColor = textColor.copy(alpha = 0.9f),
    unfocusedBorderColor = textColor.copy(alpha = unfocusedBorderAlpha),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    cursorColor = textColor
)

object TripUiFormatter {
    fun transactionTitle(type: Int, strings: AppStrings): String {
        return when (type) {
            0x01 -> strings.fareSubway
            0x02 -> strings.chargeTopUp
            0x0F, 0x0D -> strings.busFare
            0x46 -> strings.purchase
            0x50 -> strings.typeLocker
            else -> "${strings.transactionLabel} (0x${"%02X".format(type)})"
        }
    }

    fun tripSummary(trip: TripRecord, strings: AppStrings): String {
        val inDisplay = trip.inStationName ?: trip.inStation
        val outDisplay = trip.outStationName ?: trip.outStation
        return when (trip.type) {
            0x02 -> "${strings.locationPrefix} $inDisplay"
            0x0F, 0x0D -> "${strings.busRoutePrefix} $inDisplay"
            0x46 -> "${strings.terminalPrefix} $inDisplay"
            0x50 -> trip.note?.takeIf { it.isNotBlank() } ?: strings.lockerUsage
            else -> "${strings.inPrefix} $inDisplay\n${strings.outPrefix} $outDisplay"
        }
    }
}
