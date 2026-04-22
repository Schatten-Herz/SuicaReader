package com.example.suicareader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transit_cards")
data class TransitCard(
    @PrimaryKey
    val idm: String, // 十六进制的卡片 IDm
    val nickname: String = "Suica",
    val balance: Int,
    val displayNumber: String? = null, // 用户自定义展示的实体卡尾号
    val themeColor: Long = 0xFF4CAF50, // 默认主题色
    val lastUpdated: Long = System.currentTimeMillis()
)
