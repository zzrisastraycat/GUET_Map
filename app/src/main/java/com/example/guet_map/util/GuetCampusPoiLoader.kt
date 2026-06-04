package com.example.guet_map.util

import android.content.Context
import android.content.pm.PackageManager
import com.example.guet_map.model.Location
import com.example.guet_map.network.AmapPlaceResponse
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过高德 Web POI 服务（与地图 SDK 同一 Key）批量拉取桂林电子科技大学花江校区兴趣点。
 * 使用关键字检索 + 周边检索多轮分页，覆盖教学楼/食堂/宿舍/图书馆/运动场/校门等。
 */
@Singleton
class GuetCampusPoiLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val sdkPoiLoader: GuetCampusAmapSdkPoiLoader
) {
    private val webClient = OkHttpClient.Builder()
        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val campusBounds = CampusBounds(
        minLat = 25.3010,
        maxLat = 25.3145,
        minLng = 110.4110,
        maxLng = 110.4245
    )

    private val knownGuideNames = setOf(
        "第十一教学楼B区", "第十一教学楼A区", "南门", "第二学生食堂",
        "花江校区图书馆", "校图书馆", "桂林电子科技大学花江校区图书馆"
    )

    suspend fun loadGuetCampusLocations(): List<Location> = withContext(Dispatchers.IO) {
        val fromSdk = sdkPoiLoader.loadGuetCampusLocations()
        if (fromSdk.isNotEmpty()) {
            return@withContext fromSdk
        }

        val key = readAmapKey()
        if (key.isBlank()) return@withContext emptyList()

        val merged = linkedMapOf<String, Location>()

        val textKeywords = buildList {
            addAll(CampusBuildingCatalog.amapTextKeywords())
            add("桂林电子科技大学花江校区")
            add("桂电花江校区")
            add("花江校区食堂")
            add("花江校区宿舍")
            add("花江校区图书馆")
            add("花江校区教学楼")
            add("花江校区实验楼")
            add("花江校区行政楼")
            add("花江校区服务中心")
            add("花江校区运动场")
            add("花江校区校门")
            (1..17).forEach { n ->
                val cn = when (n) {
                    1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; 7 -> "七"
                    8 -> "八"; 9 -> "九"; 10 -> "十"; 11 -> "十一"; 12 -> "十二"; 13 -> "十三"
                    14 -> "十四"; 15 -> "十五"; 16 -> "十六"; 17 -> "十七"
                    else -> n.toString()
                }
                add("${n}教")
                add("${cn}教")
                add("第${cn}教学楼")
                add("第${n}教学楼")
                add("${n}号楼")
                add("${cn}号楼")
                add("桂林电子科技大学花江校区第${cn}教学楼")
            }
        }
        for (keyword in textKeywords) {
            fetchTextPages(key, keyword).forEach { loc ->
                merged[loc.dedupeKey()] = loc
            }
            delay(150)
        }

        val aroundKeywords = listOf(
            "教学楼", "教", "食堂", "宿舍", "公寓", "图书馆", "体育", "运动场",
            "操场", "校门", "南门", "北门", "咖啡", "超市", "学院"
        )
        for (keyword in aroundKeywords) {
            fetchAroundPages(key, keyword).forEach { loc ->
                merged[loc.dedupeKey()] = loc
            }
            delay(150)
        }

        merged.values.sortedBy { it.name }
    }

    private fun fetchTextPages(key: String, keyword: String): List<Location> {
        val collected = mutableListOf<Location>()
        repeat(MAX_PAGES) { page ->
            val url = buildString {
                append("https://restapi.amap.com/v3/place/text?")
                append("keywords=${URLEncoder.encode(keyword, Charsets.UTF_8.name())}")
                append("&city=桂林&citylimit=true")
                append("&offset=$PAGE_SIZE&page=${page + 1}")
                append("&key=$key")
            }
            val pois = requestPois(url) ?: return collected
            if (pois.isEmpty()) return collected
            pois.mapNotNull { it.toLocationOrNull() }.forEach { collected.add(it) }
            if (pois.size < PAGE_SIZE) return collected
            Thread.sleep(120)
        }
        return collected
    }

    private fun fetchAroundPages(key: String, keyword: String): List<Location> {
        val collected = mutableListOf<Location>()
        val center = "${CampusGeo.CENTER_LNG},${CampusGeo.CENTER_LAT}"
        repeat(MAX_PAGES) { page ->
            val url = buildString {
                append("https://restapi.amap.com/v3/place/around?")
                append("location=$center")
                append("&keywords=${URLEncoder.encode(keyword, Charsets.UTF_8.name())}")
                append("&radius=$AROUND_RADIUS_METERS")
                append("&offset=$PAGE_SIZE&page=${page + 1}")
                append("&key=$key")
            }
            val pois = requestPois(url) ?: return collected
            if (pois.isEmpty()) return collected
            pois.mapNotNull { it.toLocationOrNull() }.forEach { collected.add(it) }
            if (pois.size < PAGE_SIZE) return collected
            Thread.sleep(120)
        }
        return collected
    }

    private fun requestPois(url: String): List<com.example.guet_map.network.AmapPlacePoi>? {
        return try {
            val response = webClient.newCall(Request.Builder().url(url).get().build()).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) return null
            val parsed = gson.fromJson(body, AmapPlaceResponse::class.java)
            if (parsed.status != "1") return null
            parsed.pois.orEmpty()
        } catch (_: Exception) {
            null
        }
    }

    private fun isOnCampus(lat: Double, lng: Double, name: String, address: String): Boolean {
        if (!campusBounds.contains(lat, lng)) return false
        val text = name + address
        if (text.contains("花江") || text.contains("桂电") || text.contains("电子科技大学")) return true
        return !text.contains("广西师范大学") && !text.contains("理工大学")
    }

    private fun com.example.guet_map.network.AmapPlacePoi.toLocationOrNull(): Location? {
        val raw = location ?: return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val lng = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        val title = name.orEmpty().ifBlank { return null }
        if (!campusBounds.contains(lat, lng)) return null
        val category = mapCategory(title, type)
        return Location(
            locationId = id?.takeIf { it.isNotBlank() } ?: "poi_${title.hashCode()}_${lat.toBits()}",
            name = title,
            latitude = lat,
            longitude = lng,
            category = category,
            rating = 4.0f,
            openingHours = "",
            imageUrl = "",
            hasGuide = knownGuideNames.any { title.contains(it) || it.contains(title) }
        )
    }

    private fun mapCategory(name: String, typeDes: String?): String {
        val text = name + (typeDes.orEmpty())
        return when {
            text.contains("食堂") || text.contains("餐厅") || text.contains("饭堂") -> "食堂"
            text.contains("图书馆") -> "图书馆"
            text.contains("宿舍") || text.contains("公寓") || text.contains("生活区") -> "宿舍"
            text.contains("教学") || Regex("[0-9一二三四五六七八九十]+教").containsMatchIn(text) -> "教室"
            text.contains("体育") || text.contains("运动") || text.contains("球场") ||
                text.contains("操场") || text.contains("田径") -> "运动场"
            text.contains("南门") || text.contains("北门") || text.contains("东门") ||
                text.contains("西门") || text.contains("校门") -> "校门"
            text.contains("咖啡") -> "咖啡"
            text.contains("超市") || text.contains("商店") || text.contains("书店") ||
                text.contains("便利店") -> "商店"
            else -> "其他"
        }
    }

    private fun Location.dedupeKey(): String =
        "${name.trim()}@${"%.5f".format(latitude)},${"%.5f".format(longitude)}"

    private fun readAmapKey(): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        return appInfo.metaData?.getString("com.amap.api.v2.apikey").orEmpty()
    }

    private data class CampusBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    ) {
        fun contains(lat: Double, lng: Double): Boolean =
            lat in minLat..maxLat && lng in minLng..maxLng
    }

    companion object {
        private const val PAGE_SIZE = 25
        private const val MAX_PAGES = 4
        private const val AROUND_RADIUS_METERS = 2500
    }
}
