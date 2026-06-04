package com.example.guet_map.util

import com.example.guet_map.model.Location

/** 校内搜索：最多 7 条；精确 → 楼号互推 → 模糊包含 */
object CampusSearchMatcher {

    private const val TIER_EXACT = 3
    private const val TIER_NUMERIC = 2
    private const val TIER_FUZZY = 1

    fun filterAndSort(
        locations: List<Location>,
        rawQuery: String,
        limit: Int = CampusSearchQueryNormalizer.MAX_SEARCH_RESULTS
    ): List<Location> {
        val ctx = CampusSearchQueryNormalizer.analyze(rawQuery)
        if (ctx.normalized.isEmpty()) return locations.take(limit)

        if (ctx.normalized == "教") {
            return locations
                .filter { isTeachingPoi(it) }
                .sortedWith(compareBy<Location> { teachingBuildingNumber(it) ?: Int.MAX_VALUE }.thenBy { it.name })
                .take(limit)
        }

        return locations
            .map { it to score(it, ctx) }
            .filter { it.second.tier > 0 }
            .sortedWith(
                compareByDescending<Pair<Location, Score>> { it.second.tier }
                    .thenByDescending { it.second.points }
                    .thenBy { teachingBuildingNumber(it.first) ?: Int.MAX_VALUE }
                    .thenBy { it.first.name }
            )
            .take(limit)
            .map { it.first }
    }

    fun resolveBest(locations: List<Location>, rawQuery: String): Location? =
        filterAndSort(locations, rawQuery, limit = 1).firstOrNull()

    private data class Score(val tier: Int, val points: Int)

    private fun score(location: Location, ctx: CampusSearchQueryNormalizer.QueryContext): Score {
        val name = location.name
        var best = Score(0, 0)

        fun consider(tier: Int, points: Int) {
            if (tier > best.tier || (tier == best.tier && points > best.points)) {
                best = Score(tier, points)
            }
        }

        if (name.equals(ctx.normalized, ignoreCase = true)) consider(TIER_EXACT, 1000)
        ctx.variants.forEach { v ->
            if (v.isNotEmpty() && name.equals(v, ignoreCase = true)) consider(TIER_EXACT, 980)
        }

        CampusBuildingCatalog.findEntryByAlias(ctx.normalized)?.let { entry ->
            if (entry.matchesName(name)) consider(TIER_EXACT, 960)
        }

        ctx.buildingNumber?.let { bn ->
            val nameBuilding = CampusBuildingNumbers.parse(name)
            val teachingEntry = CampusBuildingCatalog.findTeachingBuilding(bn)
            if (nameBuilding == bn) {
                consider(TIER_NUMERIC, 760 + if (name.contains("教学")) 120 else 80)
            }
            CampusSearchQueryNormalizer.expandBuildingVariants(bn).forEach { v ->
                if (name.contains(v, ignoreCase = true)) consider(TIER_NUMERIC, 700 + v.length)
            }
            teachingEntry?.let { entry ->
                if (entry.matchesName(name)) consider(TIER_NUMERIC, 780)
            }
            CampusBuildingCatalog.findEntryByAlias("${bn}教")?.let { entry ->
                if (entry.matchesName(name)) consider(TIER_NUMERIC, 740)
            }
        }

        ctx.variants.forEach { v ->
            if (v.length >= 2 && name.contains(v, ignoreCase = true)) {
                val tier = if (ctx.buildingNumber != null && isTeachingPoi(location)) {
                    TIER_NUMERIC
                } else {
                    TIER_FUZZY
                }
                consider(tier, 320 + v.length * 6)
            }
        }

        if (ctx.normalized.length >= 1 && name.contains(ctx.normalized, ignoreCase = true)) {
            consider(TIER_FUZZY, 200 + ctx.normalized.length * 10)
        }

        CampusDormitoryCatalog.findZoneByQuery(ctx.normalized)?.let { zone ->
            if (CampusDormitoryCatalog.matchesLocation(location, zone)) consider(TIER_EXACT, 900)
        }

        return best
    }

    private fun isTeachingPoi(location: Location): Boolean {
        val name = location.name
        return location.category == "教室" || name.contains("教学") ||
            CampusBuildingNumbers.parse(name) != null
    }

    private fun teachingBuildingNumber(location: Location): Int? {
        CampusBuildingNumbers.parse(location.name)?.let { return it }
        CampusBuildingCatalog.findEntryByAlias(location.name)?.let { entry ->
            return entry.locationId.removePrefix("building_").takeWhile { it.isDigit() }.toIntOrNull()
        }
        return null
    }
}
