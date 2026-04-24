package com.example.suicareader.ui.map

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

object TransitMapCatalog {
    @Volatile
    private var initialized = false
    private var stationCoordinates: Map<String, LatLng> = emptyMap()

    private val companyColors = mapOf(
        // JR
        "東日本旅客鉄道" to Color(0xFF48A868),
        "東海旅客鉄道" to Color(0xFFF28C28),
        "西日本旅客鉄道" to Color(0xFF2A6FD3),
        "北海道旅客鉄道" to Color(0xFF1B5E20),
        "九州旅客鉄道" to Color(0xFFE53935),
        "四国旅客鉄道" to Color(0xFF26A69A),

        // Tokyo major
        "東京都交通局" to Color(0xFFE85D9A),
        "東京メトロ" to Color(0xFF2C7BE5),
        "京成電鉄" to Color(0xFF2A59C6),
        "東急電鉄" to Color(0xFFE53935),
        "小田急電鉄" to Color(0xFF1E88E5),
        "京王電鉄" to Color(0xFFDD2C00),
        "京浜急行電鉄" to Color(0xFFD32F2F),
        "西武鉄道" to Color(0xFF1565C0),
        "東武鉄道" to Color(0xFF0D47A1),
        "相模鉄道" to Color(0xFF1A237E),
        "横浜市交通局" to Color(0xFF1976D2),

        // Kansai major
        "大阪市高速電気軌道" to Color(0xFFE53935),
        "阪急電鉄" to Color(0xFF7B1FA2),
        "阪神電気鉄道" to Color(0xFF3949AB),
        "近畿日本鉄道" to Color(0xFFEF5350),
        "南海電気鉄道" to Color(0xFF43A047),
        "京阪電気鉄道" to Color(0xFF388E3C),
        "神戸市交通局" to Color(0xFF00897B),
        "京都市交通局" to Color(0xFF8E24AA),

        // Nagoya / Fukuoka
        "名古屋市交通局" to Color(0xFF00897B),
        "西日本鉄道" to Color(0xFF00ACC1)
    )

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            stationCoordinates = loadCoordinatesFromAssets(context)
            initialized = true
        }
    }

    private fun loadCoordinatesFromAssets(context: Context): Map<String, LatLng> {
        return try {
            val jsonStr = context.assets.open("station_coordinates.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, LatLng>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val coord = json.optJSONObject(key) ?: continue
                val lat = coord.optDouble("lat", Double.NaN)
                val lng = coord.optDouble("lng", Double.NaN)
                if (!lat.isNaN() && !lng.isNaN()) {
                    map[key] = LatLng(lat, lng)
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun coordinateForStation(rawName: String?): LatLng? {
        if (rawName.isNullOrBlank()) return null
        val plainName = rawName
            .substringBefore(" (")
            .substringBefore("（")
            .trim()
        val normalized = when (plainName) {
            "两国" -> "両国"
            "浅草桥" -> "浅草橋"
            "银座" -> "銀座"
            "东京" -> "東京"
            "台场海滨公园" -> "お台場海浜公園"
            else -> plainName
        }
        return stationCoordinates[normalized]
    }

    fun companyName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return "Unknown"
        val inParen = rawName
            .substringAfter("(", rawName.substringAfter("（", ""))
            .substringBefore(")")
            .substringBefore("）")
            .trim()
        if (inParen.isBlank()) return "Unknown"
        val token = inParen.substringBefore(" ").trim()
        return when (token) {
            "JR東日本" -> "東日本旅客鉄道"
            "JR東海" -> "東海旅客鉄道"
            "JR西日本" -> "西日本旅客鉄道"
            "JR北海道" -> "北海道旅客鉄道"
            "JR四国" -> "四国旅客鉄道"
            "JR九州" -> "九州旅客鉄道"
            "都営" -> "東京都交通局"
            else -> token
        }
    }

    fun colorForCompany(company: String): Color {
        return companyColors[company] ?: Color(0xFF7E57C2)
    }
}
