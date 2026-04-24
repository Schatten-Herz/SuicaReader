package com.example.suicareader.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_records",
    indices = [
        Index(value = ["cardIdm", "blockHex"], unique = true)
    ]
)
data class TripRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardIdm: String, // 关联的卡片 IDm
    val timestamp: Long, // 交易时间 (秒或毫秒)
    val type: Int, // 交易类型，如 0x01 (Fare), 0x02 (Charge)
    val inStation: String, // 进站代码或名称
    val outStation: String, // 出站代码或名称
    val inStationName: String?, // 解析后的真实进站名称
    val outStationName: String?, // 解析后的真实出站名称
    val amount: Int, // 交易金额 (可能为正或负)
    val balance: Int, // 交易后余额
    val blockHex: String, // 16字节原始数据指纹防重
    val customTitle: String? = null, // 用户自定义行程名称
    val note: String? = null // 行程备注
)
