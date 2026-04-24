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
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        return map.entries
            .asSequence()
            .filter { (_, name) ->
                // Filter out testing data from station master.
                !name.contains("試験")
            }
            .filter { (_, name) ->
                val matchesQuery = name.contains(q, ignoreCase = true)
                val matchesCompany = companyFilter == null || name.contains(companyFilter, ignoreCase = true)
                matchesQuery && matchesCompany
            }
            .map { it.key to it.value }
            .distinctBy { it.second } // remove duplicate entries from short/full codes
            .sortedWith(compareBy<Pair<String, String>> {
                val stationOnly = it.second.substringBefore(" (")
                when {
                    stationOnly.equals(q, ignoreCase = true) -> 0
                    stationOnly.startsWith(q, ignoreCase = true) -> 1
                    it.second.contains("京急川崎") && q.contains("川崎") -> 1
                    stationOnly.contains(q, ignoreCase = true) -> 2
                    else -> 3
                }
            }.thenBy { it.second.length })
            .take(300)
            .toList()
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
    val majorBusCompanies = listOf(
        "Toei Bus (都営バス)", "Keio Bus (京王バス)", "Kanto Bus (関東バス)", "Seibu Bus (西武バス)",
        "Tobu Bus (東武バス)", "Odakyu Bus (小田急バス)", "Tokyu Bus (東急バス)", "Keisei Bus (京成バス)",
        "Kanagawa Chuo Kotsu (神奈川中央交通)", "Kokusai Kogyo Bus (国際興業バス)", "Kyoto City Bus (京都市バス)",
        "Osaka City Bus (大阪シティバス)", "Nagoya City Bus (名古屋市営バス)", "Fukuoka Nishitetsu Bus (西鉄バス)",
        "JR Bus Kanto (JRバス関東)", "JR Bus Tohoku (JRバス東北)", "JR Bus Tokai (JR東海バス)"
    )

    fun searchBusCompanies(query: String): List<String> {
        return majorBusCompanies.filter { 
            query.isBlank() || it.contains(query, ignoreCase = true) 
        }
    }
}
