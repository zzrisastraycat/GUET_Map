package com.example.guet_map.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.model.WalkRouteInfo
import com.example.guet_map.util.CampusGeo
import com.example.guet_map.util.CampusLocationResolver
import com.example.guet_map.util.CampusSearchMatcher
import com.example.guet_map.util.CampusSearchQueryNormalizer
import com.example.guet_map.util.CampusWalkRoutePlanner
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.repository.FavoriteRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val guideRepository: GuideRepository,
    private val favoriteRepository: FavoriteRepository,
    private val walkRoutePlanner: CampusWalkRoutePlanner,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _walkRoute = MutableStateFlow<WalkRouteInfo?>(null)
    val walkRoute: StateFlow<WalkRouteInfo?> = _walkRoute.asStateFlow()

    private val _routeLoading = MutableStateFlow(false)
    val routeLoading: StateFlow<Boolean> = _routeLoading.asStateFlow()

    private val _routeError = MutableSharedFlow<String>()
    val routeError = _routeError.asSharedFlow()

    init {
        favoriteRepository.switchUser(userPrefs.userId)
        viewModelScope.launch {
            try {
                locationRepository.getLocations().first { it !is Resource.Loading }
                if (userPrefs.isLoggedIn) {
                    favoriteRepository.syncFromServer()
                }
            } catch (_: Exception) {
            }
        }
    }

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

    val favoriteIds: StateFlow<Set<String>> = favoriteRepository
        .observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── 搜索 ─────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Location>> = _searchQuery
        .combine(cachedLocations) { query, locations ->
            CampusSearchMatcher.filterAndSort(
                locations,
                query,
                limit = CampusSearchQueryNormalizer.MAX_SEARCH_RESULTS
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _searchQuery.value = q
        val match = resolveSearchLocation(q) ?: return
        pickFromSearch(match)
    }

    /** 搜索选中：定位地图，展开详情卡，清空导航栏，并收起搜索栏 */
    fun pickFromSearch(location: Location) {
        val target = cachedLocations.value.find { it.locationId == location.locationId } ?: location
        _highlightedLocationId.value = target.locationId
        selectLocation(target)
        clearWalkRoute()
        viewModelScope.launch {
            _events.emit(MapEvent.FocusLocation(target))
            _events.emit(MapEvent.DismissSearchUi)
        }
    }

    fun resolveSearchLocation(query: String): Location? =
        CampusLocationResolver.resolveForQuery(query, cachedLocations.value)

    fun focusOnLocation(location: Location) {
        val target = cachedLocations.value.find { it.locationId == location.locationId } ?: location
        _highlightedLocationId.value = target.locationId
        selectLocation(target)
        viewModelScope.launch {
            _events.emit(MapEvent.FocusLocation(target))
        }
    }

    fun updateMapMarkersFromCache() {
        addMarkersForLocations(cachedLocations.value)
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
        if (location.hasGuide) {
            loadGuideSteps(location.locationId)
        } else {
            _guideStepsResource.value = Resource.Success(emptyList())
        }
        viewModelScope.launch {
            _events.emit(MapEvent.ShowBottomSheet(location))
        }
    }

    fun selectLocationById(locationId: String) {
        viewModelScope.launch {
            resolveAndSelectLocation(locationId)
        }
    }

    /** 解析地点（内存 → Room → 必要时拉取列表），选中并返回；失败返回 null */
    suspend fun resolveAndSelectLocation(locationId: String): Location? {
        val resolved = resolveLocation(locationId)
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "H4",
            "MapViewModel.resolveAndSelectLocation",
            if (resolved != null) "resolved" else "resolve failed",
            mapOf(
                "locationId" to locationId,
                "cacheSize" to cachedLocations.value.size,
                "resolved" to (resolved != null)
            ),
            runId = "post-fix"
        )
        // #endregion
        resolved?.let { selectLocation(it) }
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "G2",
            "MapViewModel.resolveAndSelectLocation",
            "coords",
            mapOf(
                "locationId" to locationId,
                "name" to (resolved?.name ?: ""),
                "lat" to (resolved?.latitude ?: 0.0),
                "lng" to (resolved?.longitude ?: 0.0)
            ),
            runId = "geo-fix"
        )
        // #endregion
        return resolved
    }

    private suspend fun resolveLocation(locationId: String): Location? {
        cachedLocations.value.find { it.locationId == locationId }?.let { return it }
        locationRepository.getCachedLocationById(locationId)?.let { return it }
        if (cachedLocations.value.isEmpty()) {
            locationRepository.getLocations().first { it !is Resource.Loading }
        }
        cachedLocations.value.find { it.locationId == locationId }?.let { return it }
        return locationRepository.getCachedLocationById(locationId)
            ?: favoriteRepository.enrichFavoriteFromCache(locationId)
    }

    suspend fun toggleFavorite(location: Location): Boolean =
        favoriteRepository.toggleFavorite(location)

    fun planWalkRouteTo(destination: Location, start: LatLng) {
        val dest = cachedLocations.value.find { it.locationId == destination.locationId }
            ?: destination
        _routeLoading.value = true
        walkRoutePlanner.planWalkRoute(
            start = start,
            end = LatLng(dest.latitude, dest.longitude),
            targetName = dest.name,
            onSuccess = { route ->
                _walkRoute.value = route
                _routeLoading.value = false
            },
            onError = { message ->
                _routeLoading.value = false
                viewModelScope.launch { _routeError.emit(message) }
            }
        )
    }

    fun clearWalkRoute() {
        _walkRoute.value = null
    }

    /** 花江校区中心（无 GPS 时的默认起点） */
    fun campusCenterLatLng(): LatLng = LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG)

    // ── Marker 管理 ──────────────────────────────────────────

    private var addedMarkers: List<com.amap.api.maps.model.Marker> = emptyList()
    private var highlightMarker: com.amap.api.maps.model.Marker? = null

    private val _highlightedLocationId = MutableStateFlow<String?>(null)
    val highlightedLocationId: StateFlow<String?> = _highlightedLocationId.asStateFlow()

    private fun addMarkersForLocations(locations: List<Location>) {
        val map = aMap ?: return
        addedMarkers.forEach { it.remove() }
        highlightMarker?.remove()
        highlightMarker = null

        addedMarkers = locations.map { loc ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(loc.latitude, loc.longitude))
                    .title(loc.name)
                    .snippet(loc.category)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker.`object` = loc
            marker
        }

        _highlightedLocationId.value?.let { id ->
            locations.find { it.locationId == id }?.let { showHighlightMarker(it) }
        }
    }

    fun showHighlightMarker(location: Location) {
        val map = aMap ?: return
        highlightMarker?.remove()
        highlightMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(location.name)
                .snippet("已选中")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .zIndex(2f)
        )
        highlightMarker?.`object` = location
    }

    override fun onCleared() {
        super.onCleared()
        aMap = null
    }
}

/** 地图相关的一次性事件 */
sealed class MapEvent {
    data class ShowBottomSheet(val location: Location) : MapEvent()
    data class FocusLocation(val location: Location) : MapEvent()
    data object HideBottomSheet : MapEvent()
    data object DismissSearchUi : MapEvent()
}
