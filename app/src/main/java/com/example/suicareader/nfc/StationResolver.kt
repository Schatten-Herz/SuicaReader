package com.example.suicareader.nfc

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Locale

object StationResolver {
    private var stations: Map<String, String>? = null
    private var stationMetaByKey: Map<String, StationMeta>? = null
    private var stationSearchAliasesByName: Map<String, Set<String>>? = null

    private data class StationMeta(
        val stationName: String,
        val companyName: String,
        val lineName: String
    )

    private val stationAliasesManual = mapOf(
        "秋葉原" to listOf("秋叶原", "akihabara"),
        "東京" to listOf("东京", "tokyo"),
        "新宿" to listOf("新宿", "shinjuku"),
        "渋谷" to listOf("涩谷", "shibuya"),
        "池袋" to listOf("池袋", "ikebukuro"),
        "上野" to listOf("上野", "ueno"),
        "品川" to listOf("品川", "shinagawa"),
        "浅草" to listOf("浅草", "asakusa"),
        "浅草橋" to listOf("浅草桥", "asakusabashi"),
        "両国" to listOf("两国", "ryogoku"),
        "銀座" to listOf("银座", "ginza"),
        "九段下" to listOf("九段下", "kudanshita"),
        "お台場海浜公園" to listOf("台场海滨公园", "odaiba", "odaibakaihinkoen")
    )

    private val jpToZhHansCharMap = mapOf(
        '両' to '两', '駅' to '站', '渋' to '涩', '沢' to '泽', '浜' to '滨', '戸' to '户',
        '岡' to '冈', '広' to '广', '亀' to '龟', '辺' to '边', '塚' to '冢', 'ヶ' to 'ケ',
        '辻' to '辻', '櫻' to '樱', '桜' to '樱', '萬' to '万', '鐵' to '铁', '鐡' to '铁',
        '電' to '电', '東' to '东', '西' to '西', '南' to '南', '北' to '北', '國' to '国',
        '京' to '京', '葉' to '叶', '橋' to '桥', '銀' to '银', '臺' to '台', '灣' to '湾',
        '濱' to '滨', '會' to '会', '應' to '应', '驛' to '站', '龍' to '龙'
    )

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
            stationMetaByKey = loadStationMeta(context)
            stationSearchAliasesByName = buildStationSearchAliases()
        } catch (e: Exception) {
            e.printStackTrace()
            stations = emptyMap()
            stationMetaByKey = emptyMap()
            stationSearchAliasesByName = emptyMap()
        }
    }

    fun getStationName(regionCode: Int?, lineCode: Int, stationCode: Int): String? {
        val map = stations ?: return null
        
        val lineHex = "%02X".format(lineCode)
        val staHex = "%02X".format(stationCode)
        
        if (regionCode != null) {
            val areaHex = "%02X".format(regionCode)
            val fullKey = "$areaHex-$lineHex-$staHex"
            stationMetaByKey?.get(fullKey)?.let { return formatDisplayName(it) }
            map[fullKey]?.let { return it }
        }
        
        val shortKey = "$lineHex-$staHex"
        stationMetaByKey?.get(shortKey)?.let { return formatDisplayName(it) }
        return map[shortKey]
            ?.let(::formatLegacyName)
    }

    private fun loadStationMeta(context: Context): Map<String, StationMeta> {
        return try {
            val result = mutableMapOf<String, StationMeta>()
            context.assets.open("StationCode.csv").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val cols = line.split(",", limit = 6)
                    if (cols.size < 6) return@forEach
                    val area = cols[0].trim().toIntOrNull(16) ?: return@forEach
                    val lineCode = cols[1].trim().toIntOrNull(16) ?: return@forEach
                    val stationCode = cols[2].trim().toIntOrNull(16) ?: return@forEach
                    val company = cols[3].trim()
                    val lineName = cols[4].trim()
                    val stationName = cols[5].trim()
                    if (stationName.isBlank() || stationName.contains("試験")) return@forEach

                    val meta = StationMeta(
                        stationName = stationName,
                        companyName = company,
                        lineName = lineName
                    )
                    val fullKey = "%02X-%02X-%02X".format(area, lineCode, stationCode)
                    val shortKey = "%02X-%02X".format(lineCode, stationCode)
                    result.putIfAbsent(fullKey, meta)
                    result.putIfAbsent(shortKey, meta)
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun formatDisplayName(meta: StationMeta): String {
        val company = shortenCompanyName(meta.companyName)
        val line = normalizeLineName(meta.lineName)
        val suffix = if (line.isBlank()) company else "$company $line"
        return "${meta.stationName} ($suffix)"
    }

    private fun formatLegacyName(raw: String): String {
        val station = raw.substringBefore(" (").trim()
        val company = raw.substringAfter("(", "").substringBefore(")").trim()
        if (station.isBlank()) return raw
        if (company.isBlank()) return station
        return "$station (${shortenCompanyName(company)})"
    }

    private fun normalizeLineName(line: String): String {
        if (line.isBlank()) return line
        return when {
            line.endsWith("線") -> line
            line.endsWith("本") -> "${line}線"
            line.endsWith("ケーブル") || line.contains("モノレール") || line.contains("ライナー") -> line
            else -> "${line}線"
        }
    }

    private fun shortenCompanyName(company: String): String {
        return when (company) {
            "東日本旅客鉄道" -> "JR東日本"
            "東海旅客鉄道" -> "JR東海"
            "西日本旅客鉄道" -> "JR西日本"
            "北海道旅客鉄道" -> "JR北海道"
            "四国旅客鉄道" -> "JR四国"
            "九州旅客鉄道" -> "JR九州"
            "東京地下鉄" -> "東京メトロ"
            "東京都交通局" -> "都営"
            else -> company
        }
    }

    fun searchStations(query: String, companyFilter: String? = null): List<Pair<String, String>> {
        val map = stations ?: return emptyList()
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val nq = normalizeSearchText(q)

        return map.entries
            .asSequence()
            .filter { (_, name) ->
                // Filter out testing data from station master.
                !name.contains("試験")
            }
            .filter { (_, name) ->
                val stationOnly = name.substringBefore(" (").trim()
                val normalizedName = normalizeSearchText(name)
                val aliasMatched = stationSearchAliasesByName
                    ?.get(stationOnly)
                    ?.any { alias -> alias.contains(nq) } == true
                val matchesQuery = name.contains(q, ignoreCase = true) ||
                    normalizedName.contains(nq) ||
                    aliasMatched
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

    private fun buildStationSearchAliases(): Map<String, Set<String>> {
        val values = stations?.values ?: return emptyMap()
        val index = mutableMapOf<String, MutableSet<String>>()
        values.forEach { display ->
            val station = display.substringBefore(" (").trim()
            if (station.isBlank()) return@forEach
            val aliases = index.getOrPut(station) { mutableSetOf() }
            aliases.add(normalizeSearchText(station))
            aliases.add(normalizeSearchText(toSimplifiedLike(station)))

            val romaji = kanaToRomaji(station)
            if (romaji.isNotBlank()) {
                aliases.add(normalizeSearchText(romaji))
            }

            stationAliasesManual[station]
                ?.map(::normalizeSearchText)
                ?.forEach { aliases.add(it) }
        }
        return index
    }

    private fun toSimplifiedLike(text: String): String {
        val sb = StringBuilder(text.length)
        text.forEach { c ->
            sb.append(jpToZhHansCharMap[c] ?: c)
        }
        return sb.toString()
    }

    private fun kanaToRomaji(text: String): String {
        if (!text.any { isKana(it) }) return ""
        val kana = text.map { katakanaToHiragana(it) }.joinToString("")
        val map = mapOf(
            "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
            "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
            "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
            "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
            "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
            "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
            "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
            "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
            "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
            "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
            "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
            "し" to "shi", "ち" to "chi", "つ" to "tsu", "ふ" to "fu", "じ" to "ji"
        )
        val mono = mapOf(
            'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
            'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
            'さ' to "sa", 'す' to "su", 'せ' to "se", 'そ' to "so",
            'た' to "ta", 'て' to "te", 'と' to "to",
            'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
            'は' to "ha", 'ひ' to "hi", 'へ' to "he", 'ほ' to "ho",
            'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
            'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
            'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
            'わ' to "wa", 'を' to "o", 'ん' to "n",
            'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
            'ざ' to "za", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
            'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
            'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
            'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
            'ー' to "-"
        )
        val out = StringBuilder()
        var i = 0
        while (i < kana.length) {
            if (i + 1 < kana.length) {
                val pair = kana.substring(i, i + 2)
                val pairRoman = map[pair]
                if (pairRoman != null) {
                    out.append(pairRoman)
                    i += 2
                    continue
                }
            }
            val c = kana[i]
            if (c == 'っ') {
                if (i + 1 < kana.length) {
                    val next = mono[kana[i + 1]] ?: ""
                    if (next.isNotEmpty()) out.append(next.first())
                }
                i++
                continue
            }
            out.append(mono[c] ?: c)
            i++
        }
        return out.toString()
    }

    private fun isKana(c: Char): Boolean {
        return c in '\u3040'..'\u30FF'
    }

    private fun katakanaToHiragana(c: Char): Char {
        return if (c in '\u30A1'..'\u30F6') (c.code - 0x60).toChar() else c
    }

    private fun normalizeSearchText(input: String): String {
        return input
            .lowercase(Locale.ROOT)
            .replace("　", " ")
            .replace("（", "(")
            .replace("）", ")")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
            .trim()
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
