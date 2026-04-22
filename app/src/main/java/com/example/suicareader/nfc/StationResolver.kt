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
        
        if (regionCode != null) {
            val areaHex = "%02X".format(regionCode)
            val fullKey = "$areaHex-$lineHex-$staHex"
            map[fullKey]?.let { return it }
        }
        
        val shortKey = "$lineHex-$staHex"
        return map[shortKey]
    }

    fun searchStations(query: String, companyFilter: String? = null): List<Pair<String, String>> {
        val map = stations ?: return emptyList()
        return map.entries
            .filter { (_, name) -> 
                val matchesQuery = query.isBlank() || name.contains(query, ignoreCase = true)
                val matchesCompany = companyFilter == null || name.contains(companyFilter, ignoreCase = true)
                matchesQuery && matchesCompany
            }
            .map { it.key to it.value }
            .take(50) // Limit results for performance
    }

    fun getAllCompanies(): List<String> {
        // We know names are "StationName (CompanyName)"
        // This is a naive extraction for demonstration
        val map = stations ?: return emptyList()
        val companies = mutableSetOf<String>()
        map.values.forEach { name ->
            val start = name.indexOf('(')
            val end = name.indexOf(')')
            if (start != -1 && end != -1 && start < end) {
                val companyAndLine = name.substring(start + 1, end)
                val company = companyAndLine.split(" ").firstOrNull() ?: companyAndLine
                companies.add(company)
            }
        }
        return companies.sorted().take(20) // Provide top 20 or we could hardcode popular ones
    }
}
