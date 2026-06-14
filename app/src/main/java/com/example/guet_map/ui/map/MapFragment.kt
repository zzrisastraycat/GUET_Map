package com.example.guet_map.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.maps.AMap
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.ServiceSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentMapBinding
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.ui.MainNavViewModel
import com.example.guet_map.ui.common.AnimationUtils
import com.example.guet_map.ui.map.component.FilterTagAdapter
import com.example.guet_map.ui.map.component.LocationBottomSheetComponent
import com.example.guet_map.ui.map.component.LocationDetailCardComponent
import com.example.guet_map.ui.map.component.NavigationPanelComponent
import com.example.guet_map.ui.map.component.SearchBarComponent
import com.example.guet_map.ui.map.state.ErrorType
import com.example.guet_map.ui.map.state.MapUiEvent
import com.example.guet_map.ui.map.state.MapUiState
import com.example.guet_map.util.CampusGeo
import com.example.guet_map.util.CoordinateUtil
import com.example.guet_map.util.FetchWeatherSafetyUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment() {

    @Inject lateinit var authRepository: com.example.guet_map.repository.AuthRepository
    @Inject lateinit var userPrefs: com.example.guet_map.data.UserPrefs
    @Inject lateinit var fetchWeatherSafetyUseCase: FetchWeatherSafetyUseCase

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()

    private var aMap: AMap? = null
    private var mapViewCreated = false

    private var aMapLocationClient: com.amap.api.location.AMapLocationClient? = null
    private var myLocationMarker: com.amap.api.maps.model.Marker? = null
    private var latestLocation: android.location.Location? = null
    private var latestGcjLatLng: LatLng? = null
    private var hasAutoCenteredOnLocation = false
    private var routePolyline: com.amap.api.maps.model.Polyline? = null

    private lateinit var filterAdapter: FilterTagAdapter
    private lateinit var searchBarComponent: SearchBarComponent
    private lateinit var navigationPanelComponent: NavigationPanelComponent
    private lateinit var locationDetailCardComponent: LocationDetailCardComponent
    private lateinit var bottomSheetComponent: LocationBottomSheetComponent

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            initAndStartAmapLocation()
        }
    }

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchVoiceRecognizer() else Toast.makeText(context, "需要麦克风权限才能使用语音搜索", Toast.LENGTH_SHORT).show()
    }

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            binding.etSearch.setText(text)
            viewModel.submitSearch(text)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponents()
        setupViews()
        setupClickListeners()
        observeViewModel()

        if (viewModel.isPrivacyAgreed) {
            initMapView(savedInstanceState)
        } else {
            showPrivacyDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapViewCreated) {
            binding.mapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mapViewCreated) {
            binding.mapView.onPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationServices()
        if (mapViewCreated) {
            binding.mapView.onDestroy()
            mapViewCreated = false
        }
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            if (mapViewCreated && _binding != null) {
                binding.mapView.onSaveInstanceState(outState)
            }
        } catch (_: Exception) {
        }
    }

    private fun initComponents() {
        searchBarComponent = SearchBarComponent(
            context = requireContext(),
            binding = binding,
            onQueryChanged = { query -> viewModel.setSearchQuery(query) },
            onSearchSubmit = { query -> viewModel.submitSearch(query) },
            onLocationPicked = { location -> viewModel.pickFromSearch(location) }
        )

        navigationPanelComponent = NavigationPanelComponent(
            context = requireContext(),
            parent = binding.mapContainer
        ).apply {
            onCloseNavigation = { viewModel.clearWalkRoute() }
            onStartNavigation = { location -> openExternalNavigation(location) }
        }

        locationDetailCardComponent = LocationDetailCardComponent(
            context = requireContext(),
            parent = binding.mapContainer
        ).apply {
            onNavigate = { location -> startWalkNavigation(location) }
            onFavorite = { location -> viewLifecycleOwner.lifecycleScope.launch { toggleFavorite(location) } }
        }

        bottomSheetComponent = LocationBottomSheetComponent(binding).apply {
            onNavigate = { location -> startWalkNavigation(location) }
            onFavorite = { location -> viewLifecycleOwner.lifecycleScope.launch { toggleFavorite(location) } }
            onShare = { location -> shareLocation(location) }
            onContributeGuide = { mainNavViewModel.requestTab(R.id.nav_contribute) }
        }
    }

    private fun setupViews() {
        val filterTags = listOf("食堂", "教室", "咖啡", "图书馆", "宿舍", "校门", "商店", "运动场")
        filterAdapter = FilterTagAdapter(filterTags) { tag ->
            viewModel.filterByCategory(tag)
        }
        binding.rvFilterTags.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabMyLocation.setOnClickListener {
            centerOnMyLocation()
        }

        binding.ivMenu.setOnClickListener {
            mainNavViewModel.requestTab(R.id.nav_login)
        }
        binding.ivAvatar.setOnClickListener {
            mainNavViewModel.requestTab(R.id.nav_login)
        }

        searchBarComponent.setup()

        binding.ivVoice.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun startVoiceSearch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchVoiceRecognizer()
    }

    private fun launchVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要搜索的地点")
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "当前设备不支持语音识别", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        handleUiEvent(event)
                    }
                }

                launch {
                    viewModel.cachedLocations.collectLatest { locations ->
                        if (locations.isNotEmpty()) {
                            viewModel.updateMapMarkersFromCache()
                        }
                    }
                }

                launch {
                    viewModel.searchResults.collectLatest { results ->
                        searchBarComponent.updateSearchResults(results)
                    }
                }

                launch {
                    viewModel.selectedLocation.collect { location ->
                        location?.let {
                            bottomSheetComponent.show(it, it.locationId in viewModel.favoriteIds.value)
                        }
                    }
                }

                launch {
                    viewModel.favoriteIds.collectLatest { ids ->
                        bottomSheetComponent.updateFavoriteState(
                            viewModel.selectedLocation.value?.locationId in ids
                        )
                    }
                }

                launch {
                    viewModel.guideStepsResource.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> bottomSheetComponent.showGuideLoading()
                            is Resource.Success -> bottomSheetComponent.showGuideSteps(resource.data)
                            is Resource.Error -> bottomSheetComponent.showGuideError(resource.message)
                        }
                    }
                }

                launch {
                    viewModel.walkRoute.collect { route ->
                        if (route != null) {
                            showWalkRouteOnMap(route)
                        } else {
                            clearWalkRouteFromMap()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainNavViewModel.pendingLocationId.collectLatest { locationId ->
                    locationId ?: return@collectLatest
                    val id = mainNavViewModel.consumePendingLocation() ?: return@collectLatest
                    openLocationOnMap(id)
                }
            }
        }
    }

    private fun handleUiState(state: MapUiState) {
        when (state) {
            is MapUiState.Idle -> {}
            is MapUiState.Loading -> {}
            is MapUiState.LocationsLoaded -> {}
            is MapUiState.SearchResult -> {}
            is MapUiState.LocationDetail -> {}
            is MapUiState.Navigating -> {
                if (state.isLoading) {
                    navigationPanelComponent.showLoading()
                } else {
                    state.route?.let {
                        navigationPanelComponent.show(state.target, it)
                    }
                }
            }
            is MapUiState.Error -> {
                handleError(state.message, state.type)
            }
        }
    }

    private fun handleUiEvent(event: MapUiEvent) {
        when (event) {
            is MapUiEvent.ShowToast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            is MapUiEvent.ShowLocationSheet -> {}
            is MapUiEvent.HideLocationSheet -> {
                bottomSheetComponent.hide()
            }
            is MapUiEvent.FocusMap -> {
                focusMap(event.latitude, event.longitude, event.zoom)
            }
            is MapUiEvent.DismissSearchInput -> {
                searchBarComponent.dismissSearchResults()
            }
            is MapUiEvent.ShowLoading -> {}
            is MapUiEvent.HideLoading -> {}
            is MapUiEvent.NavigateToExternal -> {
                openExternalNavigationByCoords(event.latitude, event.longitude, event.name)
            }
            is MapUiEvent.RequestLocationPermission -> {
                requestLocationPermission()
            }
            is MapUiEvent.ShowLocationAccuracy -> {}
        }
    }

    private fun handleError(message: String, type: ErrorType) {
        val contextMessage = when (type) {
            ErrorType.NETWORK_ERROR -> getString(R.string.error_network)
            ErrorType.LOCATION_PERMISSION_DENIED -> getString(R.string.error_location_permission)
            ErrorType.LOCATION_FAILED -> getString(R.string.error_location)
            ErrorType.ROUTE_PLAN_FAILED -> getString(R.string.error_route_planning)
            ErrorType.LOAD_DATA_FAILED -> getString(R.string.error_load_data)
            ErrorType.UNKNOWN -> message
        }
        Toast.makeText(requireContext(), contextMessage, Toast.LENGTH_SHORT).show()
        viewModel.clearError()
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        if (mapViewCreated) return

        binding.mapView.onCreate(savedInstanceState)
        mapViewCreated = true

        binding.mapView.map?.let { map ->
            aMap = map
            viewModel.aMap = map
            configureMap(map)
            setupMarkerClickListener(map)
            initNavigationClient()
            viewModel.loadLocations()
            requestLocationPermissionIfNeeded()
        }
    }

    private fun configureMap(map: AMap) {
        map.isMyLocationEnabled = false
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isScaleControlsEnabled = true
            isMyLocationButtonEnabled = false
            isRotateGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = true
            isZoomGesturesEnabled = true
            setAllGesturesEnabled(true)
        }

        map.mapType = AMap.MAP_TYPE_NORMAL

        val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
            LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG), 16f
        )
        map.moveCamera(cameraUpdate)
    }

    private fun setupMarkerClickListener(map: AMap) {
        map.setOnMarkerClickListener { marker ->
            val location = marker.`object` as? Location
            if (location != null) {
                viewModel.selectLocation(location)
            }
            false
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            initAndStartAmapLocation()
            return
        }

        val shouldShowRationale =
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (shouldShowRationale) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("需要定位权限")
                .setMessage("GUET地图需要获取您的位置信息，以便在校园地图上显示您的当前位置，提供精准的导航服务。")
                .setPositiveButton("去授权") { _, _ ->
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                .setNegativeButton("暂不", null)
                .show()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestLocationPermission() {
        requestLocationPermissionIfNeeded()
    }

    private fun stopLocationServices() {
        aMapLocationClient?.stop()
        aMapLocationClient?.destroy()
        aMapLocationClient = null
    }

    private fun initAndStartAmapLocation() {
        aMapLocationClient = AMapLocationClient(requireContext())
        aMapLocationClient?.onLocationResult = { amapLocation ->
            onAmapLocationReceived(amapLocation)
        }
        aMapLocationClient?.onLocationError = { _, _ -> }
        aMapLocationClient?.start()
    }

    private fun onAmapLocationReceived(amapLocation: com.amap.api.location.AMapLocation) {
        val location = AMapLocationClient.toStandardLocation(amapLocation)
        onLocationReceived(location, amapLocation)
    }

    private fun onLocationReceived(location: android.location.Location, amapLocation: com.amap.api.location.AMapLocation) {
        val gcj = CoordinateUtil.wgs84ToGcj02(
            requireContext(),
            location.latitude,
            location.longitude
        )
        latestLocation = location
        latestGcjLatLng = LatLng(gcj.latitude, gcj.longitude)
        val map = aMap ?: return
        val latLng = latestGcjLatLng!!

        if (myLocationMarker == null) {
            myLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
                    .anchor(0.5f, 0.5f)
                    .zIndex(10f)
            )
        } else {
            myLocationMarker?.position = latLng
        }

        if (!hasAutoCenteredOnLocation && amapLocation.accuracy < 50) {
            hasAutoCenteredOnLocation = true
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latLng, 17f)
            map.animateCamera(update, 500, null)
        }
    }

    private fun centerOnMyLocation() {
        val map = aMap ?: return
        val loc = latestLocation
        if (loc != null) {
            val gcj = CoordinateUtil.wgs84ToGcj02(requireContext(), loc.latitude, loc.longitude)
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(gcj.latitude, gcj.longitude), 17f
            )
            map.moveCamera(update)
        } else {
            requestLocationPermissionIfNeeded()
            Toast.makeText(requireContext(), "正在获取位置…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initNavigationClient() {
        // Navigation client initialization placeholder
    }

    private fun startWalkNavigation(location: Location) {
        val start = latestGcjLatLng ?: viewModel.campusCenterLatLng().also {
            Toast.makeText(requireContext(), R.string.route_no_location, Toast.LENGTH_SHORT).show()
        }
        viewModel.planWalkRouteTo(location, start)
    }

    private fun openExternalNavigation(location: Location) {
        val pm = requireContext().packageManager
        try {
            val uriBuilder = StringBuilder("androidamap://route/plan/?")
            uriBuilder.append("dlat=${location.latitude}&dlon=${location.longitude}")
            uriBuilder.append("&dname=${Uri.encode(location.name)}")
            uriBuilder.append("&dev=0&t=2")

            if (latestGcjLatLng != null) {
                uriBuilder.append("&slat=${latestGcjLatLng?.latitude}&slon=${latestGcjLatLng?.longitude}")
                uriBuilder.append("&sname=${Uri.encode("我的位置")}")
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
                setPackage("com.autonavi.minimap")
            }

            if (intent.resolveActivity(pm) != null) {
                startActivity(intent)
            } else {
                openGenericMap(location)
            }
        } catch (_: Exception) {
            openGenericMap(location)
        }
    }

    private fun openExternalNavigationByCoords(lat: Double, lng: Double, name: String) {
        val pm = requireContext().packageManager
        try {
            val uriBuilder = StringBuilder("androidamap://route/plan/?")
            uriBuilder.append("dlat=$lat&dlon=$lng")
            uriBuilder.append("&dname=${Uri.encode(name)}")
            uriBuilder.append("&dev=0&t=2")

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
                setPackage("com.autonavi.minimap")
            }

            if (intent.resolveActivity(pm) != null) {
                startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }

    private fun openGenericMap(location: Location) {
        val geoUri = Uri.parse(
            "geo:${location.latitude},${location.longitude}?q=${Uri.encode(location.name)}"
        )
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.nav_amap_app)))
        } else {
            copyLocationToClipboard(location)
        }
    }

    private fun copyLocationToClipboard(location: Location) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(
            "坐标",
            "${location.name}: ${location.latitude}, ${location.longitude}"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "坐标已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun showWalkRouteOnMap(route: com.example.guet_map.model.WalkRouteInfo) {
        val map = aMap ?: return
        clearWalkRouteFromMap()

        routePolyline = map.addPolyline(
            com.amap.api.maps.model.PolylineOptions()
                .addAll(route.polyline)
                .width(12f)
                .color(ContextCompat.getColor(requireContext(), R.color.primary))
        )

        val minutes = (route.durationSeconds / 60).coerceAtLeast(1)
        binding.root.findViewById<android.widget.TextView>(R.id.tvRouteSummary)?.text =
            getString(R.string.route_summary_format, route.targetName, route.distanceMeters, minutes)
        binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility = View.VISIBLE

        val builder = com.amap.api.maps.model.LatLngBounds.builder()
        route.polyline.forEach { builder.include(it) }
        map.animateCamera(
            com.amap.api.maps.CameraUpdateFactory.newLatLngBounds(builder.build(), 120)
        )
    }

    private fun clearWalkRouteFromMap() {
        routePolyline?.remove()
        routePolyline = null
        binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility = View.GONE
    }

    private fun focusMap(lat: Double, lng: Double, zoom: Float) {
        aMap?.moveCamera(
            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom)
        )
    }

    private suspend fun toggleFavorite(location: Location) {
        val nowFavorite = viewModel.toggleFavorite(location)
        Toast.makeText(
            requireContext(),
            if (nowFavorite) R.string.favorite_added else R.string.favorite_removed,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareLocation(location: Location) {
        val text = buildString {
            appendLine(location.name)
            appendLine("分类：${location.category}")
            appendLine("坐标：${location.latitude}, ${location.longitude}")
            append("来自 GUET Map 校园导航")
        }
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        )
    }

    private fun openLocationOnMap(locationId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val loc = viewModel.resolveAndSelectLocation(locationId)
            if (loc != null) {
                aMap?.moveCamera(
                    com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(loc.latitude, loc.longitude), 17f
                    )
                )
            }
        }
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("隐私政策与用户协议")
            .setMessage(
                "欢迎使用GUET地图！\n\n" +
                        "我们将使用高德地图SDK为您提供定位与地图导航服务。" +
                        "在使用过程中，我们需要收集您的位置信息以提供精准的校内导航指引。\n\n" +
                        "您的位置数据仅用于本应用内的地图展示与导航功能，" +
                        "不会用于其他商业用途。\n\n" +
                        "点击「同意」即表示您已阅读并接受我们的《隐私政策》与《用户协议》。"
            )
            .setPositiveButton("同意") { _, _ ->
                viewModel.setPrivacyAgreed()
                MapsInitializer.updatePrivacyShow(requireContext(), true, true)
                MapsInitializer.updatePrivacyAgree(requireContext(), true)
                ServiceSettings.updatePrivacyShow(requireContext(), true, true)
                ServiceSettings.updatePrivacyAgree(requireContext(), true)
                initMapView(null)
            }
            .setNegativeButton("拒绝") { _, _ ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("功能受限")
                    .setMessage("您需要同意隐私政策才能使用完整的地图服务。")
                    .setPositiveButton("确定") { _, _ ->
                        requireActivity().finish()
                    }
                    .show()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private object AMapLocationClient {
            fun requireContext(): android.content.Context = throw IllegalStateException("Not initialized")
            var onLocationResult: ((com.amap.api.location.AMapLocation) -> Unit)? = null
            var onLocationError: ((Int, String) -> Unit)? = null
            fun start() {}
            fun stop() {}
            fun destroy() {}
            fun toStandardLocation(amapLocation: com.amap.api.location.AMapLocation): android.location.Location {
                return android.location.Location("").apply {
                    latitude = amapLocation.latitude
                    longitude = amapLocation.longitude
                    accuracy = amapLocation.accuracy
                }
            }
        }
    }
}
