package com.example.suicareader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.suicareader.data.db.dao.CardDao
import com.example.suicareader.data.db.entity.TransitCard
import com.example.suicareader.nfc.StationResolver
import com.example.suicareader.nfc.FeliCaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(private val cardDao: CardDao) : ViewModel() {

    // 从数据库读取的所有卡片，并转换为 StateFlow 供 Compose 监听
    val cards: StateFlow<List<TransitCard>> = cardDao.getAllCards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        ensureTestCardSeeded()
        refreshLegacyStationNames()
    }

    private fun refreshLegacyStationNames() {
        viewModelScope.launch(Dispatchers.IO) {
            val allCards = cardDao.getAllCardsList()
            allCards.forEach { card ->
                val trips = cardDao.getTripsListForCard(card.idm)
                if (trips.isEmpty()) return@forEach

                val updatedTrips = trips.map { trip ->
                    val refreshedIn = refreshNameFromCode(trip.inStation, trip.inStationName)
                    val refreshedOut = refreshNameFromCode(trip.outStation, trip.outStationName)
                    if (refreshedIn != trip.inStationName || refreshedOut != trip.outStationName) {
                        trip.copy(inStationName = refreshedIn, outStationName = refreshedOut)
                    } else {
                        trip
                    }
                }

                if (updatedTrips != trips) {
                    cardDao.updateTrips(updatedTrips)
                }
            }
        }
    }

    private fun refreshNameFromCode(code: String, currentName: String?): String? {
        if (!shouldRefreshStationName(currentName)) return currentName
        val parts = code.split("-")
        if (parts.size != 2) return currentName
        val lineCode = parts[0].toIntOrNull(16) ?: return currentName
        val stationCode = parts[1].toIntOrNull(16) ?: return currentName
        return StationResolver.getStationName(regionCode = null, lineCode = lineCode, stationCode = stationCode)
            ?: currentName
    }

    private fun shouldRefreshStationName(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        val token = name.substringAfter("(", "").substringBefore(")").trim()
        if (token.isBlank()) return true
        // Already has detailed "company + line" style
        if (token.contains(" ")) return false
        // Legacy full company names or old CN labels should be upgraded.
        return true
    }

    private fun ensureTestCardSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val testIdm = "TESTTOKYO2400"
            val existing = cardDao.getCardByIdm(testIdm)
            if (existing == null) {
                cardDao.insertCard(
                    TransitCard(
                        idm = testIdm,
                        nickname = "Suica Card",
                        balance = 2400,
                        themeColor = 0xFF4CAF50,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

            val existingTrips = cardDao.getTripsListForCard(testIdm)
            if (existingTrips.isNotEmpty()) {
                val migrated = existingTrips.map { trip ->
                    if (!trip.blockHex.startsWith("SEED-")) {
                        trip
                    } else {
                        trip.copy(
                            inStationName = normalizeSeedStationName(trip.inStationName),
                            outStationName = normalizeSeedStationName(trip.outStationName)
                        )
                    }
                }
                if (migrated != existingTrips) {
                    cardDao.updateTrips(migrated)
                }
                return@launch
            }

            val now = System.currentTimeMillis()
            val dayMs = 24L * 60L * 60L * 1000L
            val random = Random(now)
            val seedTrips = mutableListOf<com.example.suicareader.data.db.entity.TripRecord>()

            fun addTrip(
                daysAgo: Int,
                minuteOffset: Int,
                type: Int,
                amount: Int,
                inName: String,
                outName: String? = null,
                title: String? = null
            ) {
                val timestamp = now - daysAgo * dayMs + minuteOffset * 60L * 1000L
                seedTrips.add(
                    com.example.suicareader.data.db.entity.TripRecord(
                        cardIdm = testIdm,
                        timestamp = timestamp,
                        type = type,
                        inStation = inName,
                        outStation = outName ?: "",
                        inStationName = inName,
                        outStationName = outName,
                        amount = amount,
                        balance = 0,
                        blockHex = "SEED-${timestamp}-${random.nextInt(1000, 9999)}",
                        customTitle = title,
                        note = null
                    )
                )
            }

            // Oldest -> newest. Amount < 0 means fare/purchase, > 0 means charge.
            addTrip(daysAgo = 3, minuteOffset = -620, type = 0x02, amount = 1000, inName = "両国 (JR東日本 総武線)", title = "充值")
            addTrip(daysAgo = 3, minuteOffset = -570, type = 0x01, amount = -178, inName = "両国 (JR東日本 総武線)", outName = "秋葉原 (JR東日本 山手線)", title = "地铁/电车")
            addTrip(daysAgo = 2, minuteOffset = -520, type = 0x0F, amount = -210, inName = "上野", outName = "浅草橋", title = "公交")
            addTrip(daysAgo = 2, minuteOffset = -470, type = 0x01, amount = -165, inName = "浅草 (東京メトロ 銀座線)", outName = "銀座 (東京メトロ 銀座線)", title = "地铁")
            addTrip(daysAgo = 1, minuteOffset = -430, type = 0x02, amount = 500, inName = "新宿 (JR東日本 山手線)", title = "充值")
            addTrip(daysAgo = 1, minuteOffset = -390, type = 0x01, amount = -220, inName = "新宿 (都営 新宿線)", outName = "九段下 (都営 新宿線)", title = "地铁")
            addTrip(daysAgo = 0, minuteOffset = -180, type = 0x01, amount = -155, inName = "両国 (JR東日本 総武線)", outName = "浅草橋 (JR東日本 総武線)", title = "地铁/电车")
            addTrip(daysAgo = 0, minuteOffset = -120, type = 0x0D, amount = -195, inName = "東京", outName = "お台場海浜公園", title = "公交")

            var balanceCursor = 2400
            val balancedTrips = seedTrips
                .sortedBy { it.timestamp }
                .reversed()
                .map { trip ->
                    val withBalance = trip.copy(balance = balanceCursor)
                    balanceCursor -= trip.amount
                    withBalance
                }
                .reversed()

            cardDao.insertTrips(balancedTrips)
        }
    }

    private fun normalizeSeedStationName(name: String?): String? {
        if (name.isNullOrBlank()) return name
        val map = mapOf(
            "两国 (JR东日本)" to "両国 (JR東日本 総武線)",
            "両国 (JR东日本)" to "両国 (JR東日本 総武線)",
            "浅草桥 (JR东日本)" to "浅草橋 (JR東日本 総武線)",
            "银座 (东京Metro)" to "銀座 (東京メトロ 銀座線)",
            "浅草 (东京Metro)" to "浅草 (東京メトロ 銀座線)",
            "新宿站 (JR东日本)" to "新宿 (JR東日本 山手線)",
            "新宿 (都营新宿线)" to "新宿 (都営 新宿線)",
            "九段下 (都营新宿线)" to "九段下 (都営 新宿線)",
            "东京站丸之内南口" to "東京",
            "台场海滨公园" to "お台場海浜公園",
            "上野公园巴士" to "上野",
            "浅草雷门前" to "浅草橋"
        )
        return map[name] ?: name
    }

    fun saveCard(suicaData: FeliCaParser.SuicaData) {
        viewModelScope.launch(Dispatchers.IO) {
            val idmHex = suicaData.idm.joinToString("") { "%02X".format(it) }
            val card = TransitCard(
                idm = idmHex,
                nickname = "Suica Card",
                balance = suicaData.balance,
                themeColor = 0xFF4CAF50, // 默认使用绿色
                lastUpdated = System.currentTimeMillis()
            )
            
            // 保存或更新卡片信息到本地数据库
            cardDao.insertCard(card)

            val tripRecords = mutableListOf<com.example.suicareader.data.db.entity.TripRecord>()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            for (i in suicaData.history.indices) {
                val parsed = suicaData.history[i]
                val prevBalance = if (i + 1 < suicaData.history.size) suicaData.history[i + 1].balance else parsed.balance
                val amount = parsed.balance - prevBalance
                val timestamp = format.parse(parsed.dateString)?.time ?: 0L

                tripRecords.add(
                    com.example.suicareader.data.db.entity.TripRecord(
                        cardIdm = idmHex,
                        timestamp = timestamp,
                        type = parsed.transactionType,
                        inStation = parsed.inStationCode,
                        outStation = parsed.outStationCode,
                        inStationName = parsed.inStationName,
                        outStationName = parsed.outStationName,
                        amount = amount,
                        balance = parsed.balance,
                        blockHex = parsed.blockHex,
                        customTitle = null,
                        note = null
                    )
                )
            }
            cardDao.insertTrips(tripRecords.reversed())
        }
    }

    fun deleteCard(idm: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.deleteTripsForCard(idm)
            cardDao.deleteCard(idm)
        }
    }

    fun updateCardInfo(idm: String, newName: String, newNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCards = cards.value
            val cardToUpdate = currentCards.find { it.idm == idm }
            if (cardToUpdate != null) {
                val updated = cardToUpdate.copy(
                    nickname = newName,
                    displayNumber = newNumber.takeIf { it.isNotBlank() }
                )
                cardDao.updateCard(updated)
            }
        }
    }

    fun addManualTrip(
        idm: String,
        type: Int,
        amount: Int,
        inStationCode: String,
        inStationName: String,
        outStationCode: String?,
        outStationName: String?,
        timestamp: Long, // User selected date's timestamp
        customTitle: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentCards = cards.value
            val card = currentCards.find { it.idm == idm } ?: return@launch
            
            val blockHex = "MANUAL-${System.currentTimeMillis()}"
            
            val inCode = inStationCode.split("-").takeLast(2).joinToString("-")
            val outCode = outStationCode?.split("-")?.takeLast(2)?.joinToString("-") ?: ""
            
            val newTrip = com.example.suicareader.data.db.entity.TripRecord(
                cardIdm = idm,
                timestamp = timestamp,
                type = type,
                inStation = inCode,
                outStation = outCode,
                inStationName = inStationName,
                outStationName = outStationName,
                amount = amount,
                balance = 0, // Will be recalculated
                blockHex = blockHex,
                customTitle = customTitle?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() }
            )
            
            cardDao.insertTrips(listOf(newTrip))
            
            // Recalculate all balances
            recalculateBalances(idm)
        }
    }

    fun reorderTripsWithinDay(idm: String, reorderedTrips: List<com.example.suicareader.data.db.entity.TripRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (reorderedTrips.isEmpty()) return@launch

            // Give them new timestamps based on their base date but offset by a millisecond to preserve the new order
            // Assuming reorderedTrips are sorted from newest to oldest in the UI
            val baseTime = reorderedTrips.last().timestamp
            val updatedTrips = reorderedTrips.reversed().mapIndexed { index, trip ->
                trip.copy(timestamp = baseTime + index)
            }

            cardDao.updateTrips(updatedTrips)

            // Recalculate balances after reordering
            recalculateBalances(idm)
        }
    }

    private suspend fun recalculateBalances(idm: String) {
        val allTrips = cardDao.getTripsListForCard(idm) // ordered ascending (oldest to newest)
        if (allTrips.isEmpty()) return
        
        // Get the current known total balance of the card to work backwards from
        val currentCards = cards.value
        val card = currentCards.find { it.idm == idm } ?: return
        
        var currentBalance = card.balance
        
        // Iterate backwards (from newest to oldest) to calculate correct balances
        val updatedTrips = allTrips.reversed().map { trip ->
            val tripWithBalance = trip.copy(balance = currentBalance)
            currentBalance -= trip.amount
            tripWithBalance
        }.reversed() // Reverse back to original order for updating if needed
        
        cardDao.updateTrips(updatedTrips)
    }

    fun getTripsForCard(idm: String) = cardDao.getTripsForCard(idm)

    fun getTripById(tripId: Long) = cardDao.getTripById(tripId)

    fun updateTripDetails(trip: com.example.suicareader.data.db.entity.TripRecord, newTitle: String, newNote: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cardDao.updateTrip(
                trip.copy(
                    customTitle = newTitle.takeIf { it.isNotBlank() },
                    note = newNote.takeIf { it.isNotBlank() }
                )
            )
        }
    }
}

class MainViewModelFactory(private val cardDao: CardDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(cardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
