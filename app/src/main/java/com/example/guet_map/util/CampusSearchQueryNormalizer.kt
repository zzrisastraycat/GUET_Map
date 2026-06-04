package com.example.guet_map.util

object CampusSearchQueryNormalizer {

    const val MAX_SEARCH_RESULTS = 7

    data class QueryContext(
        val normalized: String,
        val variants: Set<String>,
        val buildingNumber: Int?,
        val teachingOnly: Boolean
    )

    fun analyze(rawQuery: String): QueryContext {
        val normalized = rawQuery.trim().replace(Regex("\\s+"), "")
        val building = CampusBuildingNumbers.parse(normalized) ?: parseBareBuildingNumber(normalized)
        val variants = linkedSetOf<String>()
        if (normalized.isNotEmpty()) variants.add(normalized)

        if (building != null) {
            expandBuildingVariants(building).forEach { variants.add(it) }
        }

        if (normalized.endsWith("教")) {
            parseBareBuildingNumber(normalized.removeSuffix("教"))?.let { n ->
                expandBuildingVariants(n).forEach { variants.add(it) }
            }
        }

        val teachingOnly = normalized == "教" ||
            (building != null && !normalized.contains("图书馆") && !normalized.contains("食堂"))
        return QueryContext(normalized, variants, building, teachingOnly)
    }

    fun parseBareBuildingNumber(query: String): Int? {
        val compact = query.trim().replace(Regex("\\s+"), "")
        Regex("^([1-9]|1[0-7])$").matchEntire(compact)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        val cnMap = listOf(
            "十七" to 17, "十六" to 16, "十五" to 15, "十四" to 14, "十三" to 13,
            "十二" to 12, "十一" to 11, "十" to 10,
            "九" to 9, "八" to 8, "七" to 7, "六" to 6, "五" to 5,
            "四" to 4, "三" to 3, "二" to 2, "一" to 1
        )
        for ((cn, n) in cnMap) {
            if (compact == cn) return n
        }
        return null
    }

    fun expandBuildingVariants(n: Int): List<String> {
        if (!CampusBuildingNumbers.isCatalogBuilding(n)) return emptyList()
        val cn = chineseOrdinal(n)
        return listOf(
            "${n}教",
            "${cn}教",
            "第${cn}教学楼",
            "第${n}教学楼",
            "${n}号楼",
            "${cn}号楼"
        )
    }

    fun chineseOrdinal(n: Int): String = when (n) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; 7 -> "七"
        8 -> "八"; 9 -> "九"; 10 -> "十"; 11 -> "十一"; 12 -> "十二"; 13 -> "十三"
        14 -> "十四"; 15 -> "十五"; 16 -> "十六"; 17 -> "十七"
        else -> n.toString()
    }
}
