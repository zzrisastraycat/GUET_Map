package com.example.guet_map.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.GuideRepository
import com.example.guet_map.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val guideRepository: GuideRepository
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "map_privacy"
        private const val KEY_PRIVACY_AGREED = "privacy_agreed"
    }

    // ── 隐私 ─────────────────────────────────────────────────

    val isPrivacyAgreed: Boolean
        get() {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_PRIVACY_AGREED, false)
        }

    fun setPrivacyAgreed() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRIVACY_AGREED, true)
            .apply()
    }

    // ── 地图 ─────────────────────────────────────────────────

    var aMap: AMap? = null

    // ── 地点数据 ─────────────────────────────────────────────

    /** 缓存的地点列表 (Room → UI 实时同步) */
    val cachedLocations: StateFlow<List<Location>> = locationRepository
        .observeCachedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _locationsResource = MutableStateFlow<Resource<List<Location>>>(Resource.Loading)
    val locationsResource: StateFlow<Resource<List<Location>>> = _locationsResource.asStateFlow()

    /** 当前筛选类别 (null = 全部) */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // ── 图文指引 ─────────────────────────────────────────────

    private val _guideStepsResource = MutableStateFlow<Resource<List<GuideStep>>>(Resource.Loading)
    val guideStepsResource: StateFlow<Resource<List<GuideStep>>> = _guideStepsResource.asStateFlow()

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation: StateFlow<Location?> = _selectedLocation.asStateFlow()

    // ── 搜索 ─────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 根据名称模糊匹配的结果 */
    val searchResults: StateFlow<List<Location>> = _searchQuery
        .combine(cachedLocations) { query, locations ->
            if (query.isBlank()) emptyList()
            else locations.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ── 事件 ─────────────────────────────────────────────────

    private val _events = MutableSharedFlow<MapEvent>()
    val events = _events.asSharedFlow()

    // ── 数据加载 ─────────────────────────────────────────────

    /** 加载所有地点 */
    fun loadLocations() {
        viewModelScope.launch {
            locationRepository.getLocations().collect { resource ->
                _locationsResource.value = resource
                if (resource is Resource.Success) {
                    addMarkersForLocations(resource.data)
                }
            }
        }
    }

    /** 按类别筛选 */
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        viewModelScope.launch {
            if (category != null) {
                locationRepository.getLocationsByCategory(category).collect { resource ->
                    _locationsResource.value = resource
                    if (resource is Resource.Success) {
                        addMarkersForLocations(resource.data)
                    }
                }
            } else {
                loadLocations()
            }
        }
    }

    /** 加载指定地点的图文指引 */
    fun loadGuideSteps(locationId: String) {
        viewModelScope.launch {
            guideRepository.getGuideSteps(locationId).collect { resource ->
                _guideStepsResource.value = resource
            }
        }
    }

    /** 选择地点 → 加载指引并展开 BottomSheet */
    fun selectLocation(location: Location) {
        _selectedLocation.value = location
        loadGuideSteps(location.locationId)
        viewModelScope.launch {
            _events.emit(MapEvent.ShowBottomSheet(location))
        }
    }

    // ── Marker 管理 ──────────────────────────────────────────

    private var addedMarkers: List<com.amap.api.maps.model.Marker> = emptyList()

    private fun addMarkersForLocations(locations: List<Location>) {
        val map = aMap ?: return
        // 清除旧标记
        addedMarkers.forEach { it.remove() }

        addedMarkers = locations.map { loc ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(loc.latitude, loc.longitude))
                    .title(loc.name)
                    .snippet(loc.category)
            )
            marker.`object` = loc  // 绑定 Location 数据到 marker
            marker
        }
    }

    override fun onCleared() {
        super.onCleared()
        aMap = null
    }
}

/** 地图相关的一次性事件 */
sealed class MapEvent {
    data class ShowBottomSheet(val location: Location) : MapEvent()
}
