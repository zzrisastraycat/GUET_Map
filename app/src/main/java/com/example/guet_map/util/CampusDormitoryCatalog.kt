package com.example.guet_map.util

import com.example.guet_map.model.Location

/**
 * 花江宿舍：苑名 ↔ 分区字母（用户校正确认）。
 * 泾苑=A区，清苑=B区，淑苑=C区，润苑=D区，溶苑=F区。
 */
object CampusDormitoryCatalog {

    data class DormZone(
        val yuanName: String,
        val zoneLetter: Char,
        val searchKeyword: String,
        val aliases: List<String>,
        val fallbackLat: Double,
        val fallbackLng: Double
    )

    val zones: List<DormZone> = listOf(
        DormZone("泾苑", 'A', "桂林电子科技大学花江校区泾苑", listOf("泾苑", "A区", "a区", "宿舍A区", "花江A区", "学生公寓A区"), 25.30620, 110.41980),
        DormZone("清苑", 'B', "桂林电子科技大学花江校区清苑", listOf("清苑", "B区", "b区", "宿舍B区", "花江B区", "学生公寓B区"), 25.30660, 110.42020),
        DormZone("淑苑", 'C', "桂林电子科技大学花江校区淑苑", listOf("淑苑", "C区", "c区", "宿舍C区", "花江C区", "学生公寓C区"), 25.30700, 110.42050),
        DormZone("润苑", 'D', "桂林电子科技大学花江校区润苑", listOf("润苑", "D区", "d区", "宿舍D区", "花江D区", "学生公寓D区"), 25.30950, 110.41680),
        DormZone("溶苑", 'F', "桂林电子科技大学花江校区溶苑", listOf("溶苑", "F区", "f区", "宿舍F区", "花江F区", "学生公寓F区"), 25.30980, 110.41720)
    )

    fun amapKeywords(): List<String> = zones.map { it.searchKeyword } + zones.map { "桂电花江${it.yuanName}" }

    fun findZoneByQuery(query: String): DormZone? {
        val q = query.trim().replace(Regex("\\s+"), "")
        if (q.isEmpty()) return null
        zones.forEach { zone ->
            if (zone.aliases.any { q.equals(it, ignoreCase = true) || q.contains(it, ignoreCase = true) }) {
                return zone
            }
            if (q.contains(zone.yuanName)) return zone
        }
        return null
    }

    fun matchesLocation(location: Location, zone: DormZone): Boolean {
        val name = location.name
        return name.contains(zone.yuanName) ||
            name.contains("${zone.zoneLetter}区", ignoreCase = true) &&
            (name.contains("宿舍") || name.contains("苑") || location.category == "宿舍")
    }

    fun scoreForZone(location: Location, zone: DormZone, query: String): Int {
        var s = 0
        val name = location.name
        if (name.contains(zone.yuanName)) s += 90
        if (location.category == "宿舍") s += 20
        if (zone.aliases.any { query.contains(it, ignoreCase = true) && name.contains(zone.yuanName) }) {
            s += 50
        }
        return s
    }

    fun locationFromZone(zone: DormZone): Location = Location(
        locationId = "dorm_${zone.zoneLetter.lowercase()}",
        name = "${zone.yuanName}（${zone.zoneLetter}区）",
        latitude = zone.fallbackLat,
        longitude = zone.fallbackLng,
        category = "宿舍",
        rating = 4.0f,
        openingHours = "",
        imageUrl = "",
        hasGuide = false
    )

    fun mergeInto(existing: List<Location>): List<Location> {
        val result = existing.toMutableList()
        for (zone in zones) {
            if (result.any { matchesLocation(it, zone) }) continue
            result.add(locationFromZone(zone))
        }
        return result.sortedBy { it.name }
    }
}
