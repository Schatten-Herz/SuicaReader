package com.example.suicareader.nfc

import android.content.Context
import android.icu.text.Transliterator
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object StationResolver {
    @Volatile
    private var initialized = false
    @Volatile
    private var stations: Map<String, String>? = null
    @Volatile
    private var stationMetaByKey: Map<String, StationMeta>? = null
    @Volatile
    private var stationSearchAliasesByName: Map<String, Set<String>>? = null
    @Volatile
    private var simplifiedPartsByDisplay: Map<String, Pair<String, String>> = emptyMap()

    private val readySignal = CompletableDeferred<Unit>()
    private val initLock = Any()

    // Kuromoji is heavyweight; build lazily and cache romaji fallbacks per station name.
    private val kuromojiTokenizer: Tokenizer by lazy { Tokenizer() }
    private val stationRomajiCache = ConcurrentHashMap<String, String>()
    private val icuTradToSimp: Transliterator? by lazy {
        try {
            Transliterator.getInstance("Traditional-Simplified")
        } catch (_: Throwable) {
            null
        }
    }

    private data class StationMeta(
        val stationName: String,
        val companyName: String,
        val lineName: String
    )

    private val stationAliasesManual = mapOf(
        // Major Tokyo / Kanto stations
        "東京" to listOf("东京", "tokyo", "tokyostation"),
        "新宿" to listOf("新宿", "shinjuku"),
        "渋谷" to listOf("涩谷", "shibuya"),
        "池袋" to listOf("池袋", "ikebukuro"),
        "上野" to listOf("上野", "ueno"),
        "品川" to listOf("品川", "shinagawa"),
        "秋葉原" to listOf("秋叶原", "akihabara", "akiba"),
        "浅草" to listOf("浅草", "浅草寺", "asakusa"),
        "浅草橋" to listOf("浅草桥", "asakusabashi"),
        "両国" to listOf("两国", "ryogoku"),
        "銀座" to listOf("银座", "ginza"),
        "新橋" to listOf("新桥", "shimbashi", "shinbashi"),
        "有楽町" to listOf("有乐町", "yurakucho"),
        "日本橋" to listOf("日本桥", "nihombashi", "nihonbashi"),
        "大手町" to listOf("大手町", "otemachi"),
        "丸の内" to listOf("丸之内", "marunouchi"),
        "原宿" to listOf("原宿", "harajuku"),
        "明治神宮前" to listOf("明治神宫前", "meijijingumae", "harajuku"),
        "表参道" to listOf("表参道", "omotesando"),
        "六本木" to listOf("六本木", "roppongi"),
        "恵比寿" to listOf("惠比寿", "ebisu"),
        "目黒" to listOf("目黑", "meguro"),
        "中目黒" to listOf("中目黑", "nakameguro"),
        "代々木" to listOf("代代木", "yoyogi"),
        "九段下" to listOf("九段下", "kudanshita"),
        "水道橋" to listOf("水道桥", "suidobashi"),
        "飯田橋" to listOf("饭田桥", "iidabashi"),
        "御茶ノ水" to listOf("御茶之水", "御茶水", "ochanomizu"),
        "浜松町" to listOf("滨松町", "hamamatsucho"),
        "大門" to listOf("大门", "daimon"),
        "高輪ゲートウェイ" to listOf("高轮gateway", "高轮门户", "takanawagateway"),
        "虎ノ門ヒルズ" to listOf("虎之门hills", "虎之门", "toranomonhills"),
        "押上" to listOf("押上", "晴空塔", "东京晴空塔", "skytree", "oshiage"),
        "とうきょうスカイツリー" to listOf("东京晴空塔", "晴空塔", "tokyoskytree", "skytree"),
        "お台場海浜公園" to listOf("台场", "台场海滨公园", "odaiba", "odaibakaihinkoen"),
        "東京テレポート" to listOf("东京电讯", "台场", "tokyoteleport", "odaiba"),
        "国際展示場" to listOf("国际展示场", "bigsight", "kokusaitenjijo"),
        "舞浜" to listOf("舞滨", "迪士尼", "东京迪士尼", "maihama", "disney", "tdr", "tdl", "tds"),
        "東京ディズニーランド" to listOf("东京迪士尼乐园", "迪士尼乐园", "disneyland", "tdl"),
        "東京ディズニーシー" to listOf("东京迪士尼海洋", "迪士尼海洋", "disneysea", "tds"),
        "羽田空港第1・第2ターミナル" to listOf("羽田机场", "羽田机场t1", "羽田机场t2", "haneda", "hnd", "hanedaairport"),
        "羽田空港第1ターミナル" to listOf("羽田机场", "羽田机场t1", "haneda", "hnd", "hanedaairport"),
        "羽田空港第2ターミナル" to listOf("羽田机场", "羽田机场t2", "haneda", "hnd", "hanedaairport"),
        "羽田空港第3ターミナル" to listOf("羽田机场", "羽田机场t3", "haneda", "hnd", "hanedaairport"),
        "羽田" to listOf("羽田", "haneda"),
        "成田空港" to listOf("成田机场", "成田机场t1", "narita", "nrt", "naritaairport"),
        "空港第2ビル" to listOf("成田机场", "成田机场t2", "成田第2航站楼", "naritaterminal2", "nrt"),
        "虎ノ門" to listOf("虎之门", "toranomon"),
        "三宮" to listOf("三宫", "神户三宫", "sannomiya"),
        "横浜" to listOf("横滨", "yokohama"),
        "新横浜" to listOf("新横滨", "shinyokohama"),
        "みなとみらい" to listOf("港未来", "横滨港未来", "minatomirai"),
        "桜木町" to listOf("樱木町", "sakuragicho"),
        "川崎" to listOf("川崎", "kawasaki"),
        "京急川崎" to listOf("京急川崎", "keikyukawasaki"),
        "大宮" to listOf("大宫", "omiya"),
        "千葉" to listOf("千叶", "chiba"),
        "鎌倉" to listOf("镰仓", "kamakura"),
        "江ノ島" to listOf("江之岛", "enoshima"),
        "小田原" to listOf("小田原", "odawara"),
        "箱根湯本" to listOf("箱根汤本", "箱根", "hakoneyumoto", "hakone"),
        "熱海" to listOf("热海", "atami"),
        "日光" to listOf("日光", "nikko"),
        "軽井沢" to listOf("轻井泽", "karuizawa"),
        "河口湖" to listOf("河口湖", "富士山", "kawaguchiko", "fuji"),
        // Major Kansai / Nationwide stations
        "関西空港" to listOf("关西机场", "关西国际机场", "关空", "kansai", "kansaiairport", "kix"),
        "大阪" to listOf("大阪", "osaka"),
        "新大阪" to listOf("新大阪", "shinosaka"),
        "大阪難波" to listOf("大阪难波", "难波", "osakanamba", "namba"),
        "難波" to listOf("难波", "namba"),
        "なんば" to listOf("难波", "namba"),
        "梅田" to listOf("梅田", "大阪梅田", "umeda"),
        "大阪梅田" to listOf("大阪梅田", "梅田", "osakaumeda", "umeda"),
        "天王寺" to listOf("天王寺", "tennoji"),
        "心斎橋" to listOf("心斋桥", "shinsaibashi"),
        "日本橋" to listOf("日本桥", "nihonbashi", "nippombashi"),
        "京橋" to listOf("京桥", "kyobashi"),
        "ユニバーサルシティ" to listOf("环球影城", "大阪环球影城", "universalcity", "usj"),
        "京都" to listOf("京都", "kyoto"),
        "祇園四条" to listOf("祇园四条", "祇园", "gionshijo", "gion"),
        "河原町" to listOf("河原町", "京都河原町", "kawaramachi"),
        "京都河原町" to listOf("京都河原町", "河原町", "kyotokawaramachi"),
        "嵐山" to listOf("岚山", "arashiyama"),
        "奈良" to listOf("奈良", "nara"),
        "近鉄奈良" to listOf("近铁奈良", "奈良", "kintetsunara", "nara"),
        "神戸" to listOf("神户", "kobe"),
        "三ノ宮" to listOf("三宫", "神户三宫", "sannomiya"),
        "神戸三宮" to listOf("神户三宫", "三宫", "kobesannomiya", "sannomiya"),
        "名古屋" to listOf("名古屋", "nagoya"),
        "中部国際空港" to listOf("中部国际机场", "名古屋机场", "centrair", "ngo"),
        "博多" to listOf("博多", "福冈", "hakata", "fukuoka"),
        "天神" to listOf("天神", "福冈天神", "tenjin"),
        "福岡空港" to listOf("福冈机场", "fukuokaairport", "fuk"),
        "札幌" to listOf("札幌", "sapporo"),
        "新千歳空港" to listOf("新千岁机场", "千岁机场", "札幌机场", "shinchitose", "cts"),
        "小樽" to listOf("小樽", "otaru"),
        "函館" to listOf("函馆", "hakodate"),
        "仙台" to listOf("仙台", "sendai"),
        "広島" to listOf("广岛", "hiroshima"),
        "長崎" to listOf("长崎", "nagasaki"),
        "熊本" to listOf("熊本", "kumamoto"),
        "鹿児島中央" to listOf("鹿儿岛中央", "鹿儿岛", "kagoshimachuo", "kagoshima"),
        "金沢" to listOf("金泽", "kanazawa"),
        "那覇空港" to listOf("那霸机场", "冲绳机场", "nahaairport", "oka")
    )

    // Exhaustive Japanese Shinjitai/Kyujitai + Traditional Chinese -> Simplified Chinese char map
    // audited against every CJK character present in app/src/main/assets/stations.json.
    private val jpToZhHansCharMap = mapOf(
        // Japanese Shinjitai / variant forms differing in Unicode from Simplified Chinese
        '両' to '两', '並' to '并', '乗' to '乘', '亀' to '龟', '仏' to '佛', '伝' to '传',
        '儀' to '仪', '児' to '儿', '円' to '圆', '剛' to '刚', '剣' to '剑', '労' to '劳',
        '動' to '动', '務' to '务', '勝' to '胜', '勢' to '势', '呂' to '吕', '呉' to '吴',
        '唄' to '呗', '問' to '问', '営' to '营', '団' to '团', '圏' to '圈', '園' to '园',
        '堅' to '坚', '場' to '场', '塚' to '冢', '塩' to '盐', '増' to '增', '売' to '卖',
        '壺' to '壶', '夢' to '梦', '姫' to '姬', '孫' to '孙', '実' to '实', '宮' to '宫',
        '岡' to '冈', '島' to '岛', '嵐' to '岚', '嶺' to '岭', '嶽' to '岳', '巌' to '岩',
        '巣' to '巢', '巻' to '卷', '師' to '师', '帯' to '带', '幣' to '币', '幾' to '几',
        '庁' to '厅', '広' to '广', '庫' to '库', '廃' to '废', '廚' to '厨', '張' to '张',
        '強' to '强', '後' to '后', '徳' to '德', '応' to '应', '恵' to '惠', '愛' to '爱',
        '戦' to '战', '戸' to '户', '拝' to '拜', '挙' to '举', '挿' to '插', '掛' to '挂',
        '採' to '采', '摂' to '摄', '斎' to '斋', '時' to '时', '曇' to '昙', '書' to '书',
        '曽' to '曾', '東' to '东', '栄' to '荣', '桜' to '樱', '検' to '检', '楡' to '榆',
        '業' to '业', '極' to '极', '楽' to '乐', '様' to '样', '樅' to '枞', '橋' to '桥',
        '櫃' to '柜', '櫛' to '栉', '櫟' to '栎', '欽' to '钦', '歩' to '步', '歳' to '岁',
        '気' to '气', '氷' to '冰', '沖' to '冲', '沢' to '泽', '浄' to '净', '浜' to '滨',
        '淵' to '渊', '渋' to '涩', '渓' to '溪', '湊' to '凑', '湯' to '汤', '満' to '满',
        '溝' to '沟', '滝' to '泷', '漢' to '汉', '濃' to '浓', '瀬' to '濑', '灘' to '滩',
        '烏' to '乌', '無' to '无', '焼' to '烧', '熱' to '热', '爺' to '爷', '現' to '现',
        '環' to '环', '甕' to '瓮', '産' to '产', '畝' to '亩', '療' to '疗', '発' to '发',
        '盤' to '盘', '県' to '县', '磯' to '矶', '種' to '种', '稲' to '稻', '穂' to '穗',
        '積' to '积', '窓' to '窗', '竜' to '龙', '竪' to '竖', '競' to '竞', '築' to '筑',
        '籠' to '笼', '糸' to '丝', '紀' to '纪', '紋' to '纹', '納' to '纳', '細' to '细',
        '終' to '终', '経' to '经', '結' to '结', '給' to '给', '絹' to '绢', '続' to '续',
        '綱' to '纲', '網' to '网', '綾' to '绫', '綿' to '绵', '総' to '总', '緑' to '绿',
        '緒' to '绪', '線' to '线', '練' to '练', '縄' to '绳', '縫' to '缝', '繋' to '系',
        '織' to '织', '羅' to '罗', '義' to '义', '習' to '习', '聖' to '圣', '臨' to '临',
        '興' to '兴', '舎' to '舍', '荘' to '庄', '華' to '华', '葉' to '叶', '蒔' to '莳',
        '蓮' to '莲', '蔵' to '藏', '薩' to '萨', '薬' to '药', '藍' to '蓝', '藪' to '薮',
        '蘇' to '苏', '蘭' to '兰', '蛍' to '萤', '螢' to '萤', '術' to '术', '衛' to '卫',
        '見' to '见', '覚' to '觉', '親' to '亲', '観' to '观', '計' to '计', '討' to '讨',
        '記' to '记', '訪' to '访', '試' to '试', '詫' to '诧', '詰' to '诘', '読' to '读',
        '調' to '调', '諏' to '诹', '諫' to '谏', '諸' to '诸', '議' to '议', '護' to '护',
        '讃' to '赞', '豊' to '丰', '貝' to '贝', '販' to '贩', '貫' to '贯', '貴' to '贵',
        '貿' to '贸', '賀' to '贺', '資' to '资', '賢' to '贤', '贄' to '贽', '跡' to '迹',
        '蹟' to '迹', '車' to '车', '軍' to '军', '軒' to '轩', '軽' to '轻', '輪' to '轮',
        '農' to '农', '辺' to '边', '連' to '连', '進' to '进', '遊' to '游', '運' to '运',
        '達' to '达', '遠' to '远', '邇' to '迩', '郵' to '邮', '郷' to '乡', '針' to '针',
        '釣' to '钓', '釧' to '钏', '鈴' to '铃', '鉄' to '铁', '鉢' to '钵', '銀' to '银',
        '銅' to '铜', '銚' to '铫', '銭' to '钱', '錦' to '锦', '鍋' to '锅', '鎌' to '镰',
        '鎚' to '锤', '鎧' to '铠', '鏡' to '镜', '鐘' to '钟', '長' to '长', '門' to '门',
        '開' to '开', '閑' to '闲', '間' to '间', '関' to '关', '陣' to '阵', '陰' to '阴',
        '陸' to '陆', '陽' to '阳', '隊' to '队', '階' to '阶', '際' to '际', '雑' to '杂',
        '難' to '难', '雲' to '云', '電' to '电', '霊' to '灵', '霧' to '雾', '須' to '须',
        '領' to '领', '頭' to '头', '額' to '额', '顔' to '颜', '願' to '愿', '風' to '风',
        '飛' to '飞', '飯' to '饭', '飾' to '饰', '餉' to '饷', '養' to '养', '餘' to '余',
        '館' to '馆', '馬' to '马', '駄' to '驮', '駅' to '站', '駒' to '驹', '駿' to '骏',
        '騎' to '骑', '騒' to '骚', '験' to '验', '騰' to '腾', '魚' to '鱼', '鮎' to '鲇',
        '鮫' to '鲛', '鯖' to '鲭', '鯨' to '鲸', '鯰' to '鲶', '鱒' to '鳟', '鳥' to '鸟',
        '鳩' to '鸠', '鳳' to '凤', '鳴' to '鸣', '鴨' to '鸭', '鴬' to '莺', '鴻' to '鸿',
        '鵜' to '鹈', '鵠' to '鹄', '鵯' to '鹎', '鶏' to '鸡', '鶯' to '莺', '鶴' to '鹤',
        '鷲' to '鹫', '鷹' to '鹰', '鷺' to '鹭', '麗' to '丽', '麺' to '面', '黒' to '黑',
        '軌' to '轨', '鐵' to '铁', '鐡' to '铁',
        // Traditional Chinese & Japanese Kyujitai/variant forms -> Simplified Chinese
        '萬' to '万', '兩' to '两', '亞' to '亚', '傳' to '传', '兒' to '儿', '區' to '区',
        '單' to '单', '參' to '参', '雙' to '双', '嚴' to '严', '國' to '国', '圓' to '圆',
        '團' to '团', '壓' to '压', '壘' to '垒', '處' to '处', '奧' to '奥', '獎' to '奖',
        '學' to '学', '實' to '实', '寶' to '宝', '寫' to '写', '專' to '专', '對' to '对',
        '壽' to '寿', '廣' to '广', '廳' to '厅', '應' to '应', '懷' to '怀', '戀' to '恋',
        '戶' to '户', '戰' to '战', '歸' to '归', '條' to '条', '來' to '来', '榮' to '荣',
        '櫻' to '樱', '樂' to '乐', '橫' to '横', '機' to '机', '淺' to '浅', '溫' to '温',
        '濕' to '湿', '澤' to '泽', '濱' to '滨', '瀧' to '泷', '瀨' to '濑', '灣' to '湾',
        '氣' to '气', '發' to '发', '髮' to '发', '鹽' to '盐', '盡' to '尽', '畫' to '画',
        '當' to '当', '眞' to '真', '禮' to '礼', '禪' to '禅', '聲' to '声', '與' to '与',
        '舊' to '旧', '臺' to '台', '莊' to '庄', '藝' to '艺', '號' to '号',
        '變' to '变', '邊' to '边', '醫' to '医', '釋' to '释', '關' to '关', '離' to '离',
        '點' to '点', '黨' to '党', '齊' to '齐', '龍' to '龙', '龜' to '龟', '驛' to '站',
        '體' to '体', '獨' to '独', '麵' to '面', '黃' to '黄', '會' to '会', '狹' to '狭',
        '勸' to '劝', '勵' to '励', '勞' to '劳', '賣' to '卖', '買' to '买', '圖' to '图',
        '廢' to '废', '彌' to '弥', '亂' to '乱', '繪' to '绘', '鄉' to '乡', '聽' to '听',
        '毎' to '每', '﨑' to '崎', '髙' to '高', 'ヶ' to 'ケ', '繫' to '系'
    )

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        synchronized(initLock) {
            if (initialized) return
            try {
                appContext.assets.open("stations.json").use { inputStream ->
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
                stationMetaByKey = loadStationMeta(appContext)
                rebuildSimplifiedDisplayCache()
                // Build fast non-Kuromoji aliases immediately so multi-language search works at startup
                stationSearchAliasesByName = buildStationSearchAliases(includeKuromoji = false)
            } catch (e: Exception) {
                e.printStackTrace()
                if (stations == null) stations = emptyMap()
                if (stationMetaByKey == null) stationMetaByKey = emptyMap()
                if (stationSearchAliasesByName == null) stationSearchAliasesByName = emptyMap()
            } finally {
                initialized = true
                readySignal.complete(Unit)
            }
        }
    }

    /**
     * Pre-warms heavy Kuromoji romaji aliases in background after core station maps are ready.
     */
    fun warmSearchIndex() {
        if (!initialized) return
        val enriched = buildStationSearchAliases(includeKuromoji = true)
        if (enriched.isNotEmpty()) {
            stationSearchAliasesByName = enriched
        }
    }

    suspend fun awaitReady() {
        readySignal.await()
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

    fun shortCompanyName(company: String): String = shortenCompanyName(company)

    fun searchStations(query: String, companyFilter: String? = null): List<Pair<String, String>> {
        val map = stations ?: return emptyList()
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val queryForms = buildQueryForms(q)
        val normalizedCompanyFilter = companyFilter
            ?.takeIf { it.isNotBlank() }
            ?.let { normalizeSearchText(toSimplifiedLike(it)) }

        return map.entries
            .asSequence()
            .filter { (_, name) ->
                // Filter out testing data from station master.
                !name.contains("試験")
            }
            .filter { (_, name) ->
                val stationOnly = name.substringBefore(" (").trim()
                val (simpStation, simpCompany) = simplifiedPartsByDisplay[name]
                    ?: splitAndSimplifyDisplay(name)
                val aliases = stationSearchAliasesByName?.get(stationOnly).orEmpty()
                val matchesQuery = name.contains(q, ignoreCase = true) ||
                    queryForms.any { qf ->
                        simpStation.contains(qf) ||
                            simpCompany.contains(qf) ||
                            aliases.any { alias -> alias.contains(qf) }
                    }
                val matchesCompany = companyFilter == null ||
                    name.contains(companyFilter, ignoreCase = true) ||
                    (normalizedCompanyFilter != null &&
                        (simpCompany.contains(normalizedCompanyFilter) || simpStation.contains(normalizedCompanyFilter)))
                matchesQuery && matchesCompany
            }
            .map { it.key to it.value }
            .distinctBy { it.second } // remove duplicate entries from short/full codes
            .sortedWith(compareBy<Pair<String, String>> {
                val stationOnly = it.second.substringBefore(" (").trim()
                val (simpStation, _) = simplifiedPartsByDisplay[it.second]
                    ?: splitAndSimplifyDisplay(it.second)
                val aliases = stationSearchAliasesByName?.get(stationOnly).orEmpty()
                when {
                    stationOnly.equals(q, ignoreCase = true) -> 0
                    queryForms.any { qf -> simpStation == qf } -> 0
                    stationOnly.startsWith(q, ignoreCase = true) -> 1
                    queryForms.any { qf -> simpStation.startsWith(qf) } -> 1
                    it.second.contains("京急川崎") && q.contains("川崎") -> 1
                    queryForms.any { qf -> aliases.contains(qf) } -> 2
                    stationOnly.contains(q, ignoreCase = true) -> 3
                    queryForms.any { qf ->
                        simpStation.contains(qf) || aliases.any { alias -> alias.startsWith(qf) }
                    } -> 3
                    else -> 4
                }
            }.thenBy { it.second.substringBefore(" (").trim().length }.thenBy { it.second.length })
            .take(300)
            .toList()
    }

    fun searchStationNames(query: String): List<String> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        return searchStations(q, null)
            .map { it.second.substringBefore(" (").trim() }
            .distinct()
            .take(200)
    }

    fun popularStationNames(): List<String> {
        val map = stations ?: return emptyList()
        val popular = listOf("東京", "新宿", "渋谷", "池袋", "品川", "秋葉原", "上野", "横浜")
        val available = map.values
            .asSequence()
            .map { it.substringBefore(" (").trim() }
            .toSet()
        return popular.filter { available.contains(it) }
    }

    data class StationCompanyOption(
        val stationCode: String,
        val companyShortName: String
    )

    fun companyOptionsForStation(stationName: String): List<StationCompanyOption> {
        val map = stations ?: return emptyList()
        return map.entries
            .asSequence()
            .map { it.key to it.value }
            .filter { (_, displayName) ->
                displayName.substringBefore(" (").trim() == stationName
            }
            .mapNotNull { (code, displayName) ->
                val companyAndLine = displayName.substringAfter("(", "").substringBefore(")").trim()
                val company = companyAndLine.substringBefore(" ").trim()
                if (company.isBlank()) null else StationCompanyOption(code, shortCompanyName(company))
            }
            .distinctBy { it.companyShortName }
            .sortedBy { it.companyShortName }
            .toList()
    }

    fun recommendedStations(companyFilter: String? = null): List<Pair<String, String>> {
        val map = stations ?: return emptyList()
        val popularStationNames = listOf(
            "東京",
            "新宿",
            "渋谷",
            "池袋",
            "品川",
            "秋葉原",
            "上野",
            "横浜"
        )

        return popularStationNames
            .asSequence()
            .flatMap { station ->
                map.entries
                    .asSequence()
                    .map { it.key to it.value }
                    .filter { (_, displayName) ->
                        val stationOnly = displayName.substringBefore(" (").trim()
                        val matchesStation = stationOnly == station
                        val matchesCompany = companyFilter == null || displayName.contains(companyFilter, ignoreCase = true)
                        matchesStation && matchesCompany
                    }
                    .take(1)
            }
            .distinctBy { it.second }
            .toList()
    }

    private fun splitAndSimplifyDisplay(display: String): Pair<String, String> {
        val stationOnly = display.substringBefore(" (").trim()
        val companyPart = display.substringAfter(" (", "").substringBeforeLast(")").trim()
        return normalizeSearchText(toSimplifiedLike(stationOnly)) to
            normalizeSearchText(toSimplifiedLike(companyPart))
    }

    private fun rebuildSimplifiedDisplayCache() {
        val values = stations?.values ?: run {
            simplifiedPartsByDisplay = emptyMap()
            return
        }
        val cache = HashMap<String, Pair<String, String>>(values.size)
        values.forEach { display ->
            if (!cache.containsKey(display)) {
                cache[display] = splitAndSimplifyDisplay(display)
            }
        }
        simplifiedPartsByDisplay = cache
    }

    private fun buildQueryForms(query: String): List<String> {
        val forms = linkedSetOf<String>()
        val rawNorm = normalizeSearchText(query)
        val simp = toSimplifiedLike(query)
        val simpNorm = normalizeSearchText(simp)
        if (rawNorm.isNotEmpty()) forms.add(rawNorm)
        if (simpNorm.isNotEmpty()) forms.add(simpNorm)

        // Strip trailing "站" / "车站" / "車站" / "駅" / "station" so e.g. "东京站" matches "東京"
        val stripped = stripStationSuffix(simp)
        if (stripped.isNotBlank() && stripped != simp) {
            val strippedNorm = normalizeSearchText(stripped)
            if (strippedNorm.isNotEmpty()) forms.add(strippedNorm)
        }

        // Expand common Chinese / English / Japanese equivalents
        expandPhraseVariants(simp).forEach { variant ->
            val norm = normalizeSearchText(variant)
            if (norm.isNotEmpty()) forms.add(norm)
        }
        if (stripped.isNotBlank() && stripped != simp) {
            expandPhraseVariants(stripped).forEach { variant ->
                val norm = normalizeSearchText(variant)
                if (norm.isNotEmpty()) forms.add(norm)
            }
        }

        // Allow Hiragana / Katakana input to match Romaji aliases as well
        if (query.any { isKana(it) }) {
            val romaji = normalizeSearchText(kanaToRomaji(query))
            if (romaji.isNotEmpty()) forms.add(romaji)
        }
        return forms.toList()
    }

    private fun stripStationSuffix(text: String): String {
        val trimmed = text.trim()
        val suffixes = listOf("车站", "車站", "駅", "站", "station")
        for (suffix in suffixes) {
            if (trimmed.length > suffix.length && trimmed.endsWith(suffix, ignoreCase = true)) {
                return trimmed.dropLast(suffix.length).trim()
            }
        }
        return trimmed
    }

    private fun expandPhraseVariants(simplifiedText: String): List<String> {
        val variants = linkedSetOf<String>()
        val replacements = listOf(
            "机场" to listOf("空港", "airport"),
            "空港" to listOf("机场", "airport"),
            "国际" to listOf("international"),
            "站前" to listOf("駅前"),
            "駅前" to listOf("站前"),
            "新干线" to listOf("shinkansen"),
            "迪士尼" to listOf("ディズニー", "disney"),
            "ディズニー" to listOf("迪士尼", "disney"),
            "环球" to listOf("ユニバーサル", "universal"),
            "ユニバーサル" to listOf("环球", "universal"),
            "航站楼" to listOf("ターミナル", "terminal"),
            "ターミナル" to listOf("航站楼", "terminal"),
            "门户" to listOf("ゲートウェイ", "gateway"),
            "ゲートウェイ" to listOf("门户", "gateway"),
            "中心" to listOf("センター", "center"),
            "公园" to listOf("パーク", "park"),
            "大学" to listOf("university"),
            "医院" to listOf("病院", "hospital"),
            "病院" to listOf("医院", "hospital"),
            "之丘" to listOf("ヶ丘", "ケ丘", "が丘"),
            "个丘" to listOf("ヶ丘", "ケ丘", "が丘"),
            "ケ丘" to listOf("之丘", "个丘", "丘"),
            "之" to listOf("ノ", "の", "ヶ", "ケ"),
            "ケ" to listOf("之", "个", "")
        )
        replacements.forEach { (from, targets) ->
            if (simplifiedText.contains(from, ignoreCase = true)) {
                targets.forEach { target ->
                    variants.add(simplifiedText.replace(from, target, ignoreCase = true))
                }
            }
        }
        return variants.toList()
    }

    private fun buildStationSearchAliases(includeKuromoji: Boolean = true): Map<String, Set<String>> {
        val values = stations?.values ?: return emptyMap()
        val index = mutableMapOf<String, MutableSet<String>>()
        values.forEach { display ->
            val station = display.substringBefore(" (").trim()
            if (station.isBlank() || station.contains("試験")) return@forEach
            val aliases = index.getOrPut(station) { mutableSetOf() }

            val simplifiedStation = toSimplifiedLike(station)
            aliases.add(normalizeSearchText(station))
            aliases.add(normalizeSearchText(simplifiedStation))
            expandPhraseVariants(simplifiedStation)
                .map(::normalizeSearchText)
                .filter { it.isNotEmpty() }
                .forEach { aliases.add(it) }

            val romaji = kanaToRomaji(station)
            if (romaji.isNotBlank() && !station.any { isKanji(it) }) {
                aliases.add(normalizeSearchText(romaji))
            } else if (includeKuromoji && station.any { isKanji(it) }) {
                // Convert mixed Kanji/Kana or pure Kanji station names via Kuromoji reading.
                val fallbackRomaji = kuromojiStationToRomaji(station)
                if (fallbackRomaji.isNotBlank()) {
                    aliases.add(normalizeSearchText(fallbackRomaji))
                }
            } else if (romaji.isNotBlank()) {
                // Before Kuromoji warms up, still keep partial kana reading searchable.
                aliases.add(normalizeSearchText(romaji))
            }

            stationAliasesManual[station]
                ?.map(::normalizeSearchText)
                ?.filter { it.isNotEmpty() }
                ?.forEach { aliases.add(it) }
        }
        return index
    }

    private fun kuromojiStationToRomaji(station: String): String {
        stationRomajiCache[station]?.let { return it }

        return try {
            val tokens = kuromojiTokenizer.tokenize(station)
            val readingParts = tokens
                .map { token ->
                    val reading = token.reading?.trim().orEmpty()
                    if (reading.isNotBlank() && reading != "*") reading else token.surface
                }
                .filter { it.isNotBlank() }

            if (readingParts.isEmpty()) {
                stationRomajiCache[station] = ""
                ""
            } else {
                val kana = readingParts.joinToString("")
                val romaji = kanaToRomaji(kana)
                stationRomajiCache[station] = romaji
                romaji
            }
        } catch (_: Exception) {
            stationRomajiCache[station] = ""
            ""
        }
    }

    private fun toSimplifiedLike(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        text.forEach { c ->
            sb.append(jpToZhHansCharMap[c] ?: c)
        }
        val mapped = sb.toString()
        val translit = icuTradToSimp ?: return mapped
        return try {
            val simp = translit.transliterate(mapped)
            // Re-run single-char map in case ICU output contains any variant form we canonicalize.
            if (simp == mapped) {
                mapped
            } else {
                val post = StringBuilder(simp.length)
                simp.forEach { c -> post.append(jpToZhHansCharMap[c] ?: c) }
                post.toString()
            }
        } catch (_: Throwable) {
            mapped
        }
    }

    private fun kanaToRomaji(text: String): String {
        if (!text.any { isKana(it) }) return ""
        val kana = text.map { katakanaToHiragana(it) }.joinToString("")
        val digraphs = mapOf(
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
            "てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu", "どぅ" to "du",
            "うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
            "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
            "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo",
            "しぇ" to "she", "じぇ" to "je", "ちぇ" to "che",
            "つぁ" to "tsa", "つぃ" to "tsi", "つぇ" to "tse", "つぉ" to "tso",
            "し" to "shi", "ち" to "chi", "つ" to "tsu", "ふ" to "fu", "じ" to "ji"
        )
        val mono = mapOf(
            'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
            'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
            'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
            'さ' to "sa", 'す' to "su", 'せ' to "se", 'そ' to "so",
            'た' to "ta", 'て' to "te", 'と' to "to",
            'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
            'は' to "ha", 'ひ' to "hi", 'へ' to "he", 'ほ' to "ho",
            'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
            'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
            'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo",
            'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
            'わ' to "wa", 'を' to "o", 'ん' to "n", 'ゔ' to "vu",
            'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
            'ざ' to "za", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
            'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
            'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
            'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
            'ヶ' to "ga", 'ー' to ""
        )
        val out = StringBuilder()
        var i = 0
        while (i < kana.length) {
            if (i + 1 < kana.length) {
                val pair = kana.substring(i, i + 2)
                val pairRoman = digraphs[pair]
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

    private fun isKanji(c: Char): Boolean {
        return c in '\u4E00'..'\u9FFF' || c in '\u3400'..'\u4DBF'
    }

    private fun isKana(c: Char): Boolean {
        return c in '\u3040'..'\u30FF'
    }

    private fun katakanaToHiragana(c: Char): Char {
        return if (c in '\u30A1'..'\u30F6') (c.code - 0x60).toChar() else c
    }

    private fun normalizeSearchText(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (rawChar in input) {
            val c = when (rawChar) {
                in 'Ａ'..'Ｚ' -> ('A'.code + (rawChar.code - 'Ａ'.code)).toChar()
                in 'ａ'..'ｚ' -> ('a'.code + (rawChar.code - 'ａ'.code)).toChar()
                in '０'..'９' -> ('0'.code + (rawChar.code - '０'.code)).toChar()
                'ヶ', 'ケ' -> 'け'
                else -> katakanaToHiragana(rawChar)
            }.lowercaseChar()
            when (c) {
                ' ', '　', '-', '_', '・', '·', '\'', '"', '（', '）', '(', ')' -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
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
        val q = query.trim()
        if (q.isBlank()) return majorBusCompanies
        val nq = normalizeSearchText(toSimplifiedLike(q))
        return majorBusCompanies.filter { company ->
            company.contains(q, ignoreCase = true) ||
                normalizeSearchText(toSimplifiedLike(company)).contains(nq)
        }
    }
}
