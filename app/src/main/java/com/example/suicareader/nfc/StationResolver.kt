package com.example.suicareader.nfc

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader

object StationResolver {
    private var stations: Map<String, String>? = null

    fun init(context: Context) {
        if (stations != null) return
        try {
            context.assets.open("stations.json").use { inputStream ->
                InputStreamReader(inputStream, "UTF-8").use { reader ->
                    val jsonStr = reader.readText()
                    val jsonObject = JSONObject(jsonStr)
                    val map = mutableMapOf<String, String>()
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = jsonObject.getString(key)
                    }
                    stations = map
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stations = emptyMap()
        }
    }

    fun getStationName(regionCode: Int?, lineCode: Int, stationCode: Int): String? {
        val map = stations ?: return null
        
        val lineHex = "%02X".format(lineCode)
        val staHex = "%02X".format(stationCode)
        
        // 尝试 Area-Line-Station 精确匹配
        if (regionCode != null) {
            val areaHex = "%02X".format(regionCode)
            val fullKey = "$areaHex-$lineHex-$staHex"
            map[fullKey]?.let { return it }
        }
        
        // 降级使用 Line-Station 匹配
        val shortKey = "$lineHex-$staHex"
        return map[shortKey]
    }
}
