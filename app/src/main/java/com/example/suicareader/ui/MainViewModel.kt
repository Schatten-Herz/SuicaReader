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
                        type = parsed.transactionType.toString(),
                        inStation = parsed.inStationCode,
                        outStation = parsed.outStationCode,
                        inStationName = parsed.inStationName,
                        outStationName = parsed.outStationName,
                        amount = amount,
                        balance = parsed.balance,
                        blockHex = parsed.blockHex
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

    fun getTripsForCard(idm: String) = cardDao.getTripsForCard(idm)
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
