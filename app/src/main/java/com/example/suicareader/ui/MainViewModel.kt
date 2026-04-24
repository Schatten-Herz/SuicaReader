package com.example.suicareader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.suicareader.data.db.dao.CardDao
import com.example.suicareader.data.db.entity.TransitCard
import com.example.suicareader.nfc.FeliCaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val cardDao: CardDao) : ViewModel() {

    // 从数据库读取的所有卡片，并转换为 StateFlow 供 Compose 监听
    val cards: StateFlow<List<TransitCard>> = cardDao.getAllCards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
