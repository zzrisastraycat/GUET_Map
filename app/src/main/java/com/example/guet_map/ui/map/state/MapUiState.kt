package com.example.guet_map.ui.map.state

import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.WalkRouteInfo

/**
 * 地图界面统一 UI 状态
 */
sealed class MapUiState {

    /** 初始状态 */
    data object Idle : MapUiState()

    /** 加载中 */
    data object Loading : MapUiState()

    /** 地点列表加载成功 */
    data class LocationsLoaded(
        val locations: List<Location>,
        val filteredLocations: List<Location>,
        val selectedCategory: String? = null
    ) : MapUiState()

    /** 搜索结果状态 */
    data class SearchResult(
        val query: String,
        val results: List<Location>
    ) : MapUiState()

    /** 地点详情展开状态 */
    data class LocationDetail(
        val location: Location,
        val isFavorite: Boolean,
        val guideSteps: List<GuideStep> = emptyList(),
        val guideLoading: Boolean = false
    ) : MapUiState()

    /** 导航状态 */
    data class Navigating(
        val target: Location,
        val route: WalkRouteInfo? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) : MapUiState()

    /** 错误状态 */
    data class Error(
        val message: String,
        val type: ErrorType = ErrorType.UNKNOWN
    ) : MapUiState()
}

/**
 * 错误类型枚举
 */
enum class ErrorType {
    /** 位置权限被拒绝 */
    LOCATION_PERMISSION_DENIED,

    /** 定位失败 */
    LOCATION_FAILED,

    /** 网络错误 */
    NETWORK_ERROR,

    /** 路径规划失败 */
    ROUTE_PLAN_FAILED,

    /** 加载数据失败 */
    LOAD_DATA_FAILED,

    /** 未知错误 */
    UNKNOWN
}
