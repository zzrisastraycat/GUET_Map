package com.example.guet_map.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ImageView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import coil.load
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.repository.AuthRepository
import com.example.guet_map.ui.MainNavViewModel
import com.example.guet_map.util.CampusGeo
import com.example.guet_map.util.CampusLocationResolver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.maps.AMap
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions

import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions

import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentMapBinding
import com.example.guet_map.databinding.LayoutNavigationPanelBinding
import com.example.guet_map.databinding.LayoutLocationDetailBinding
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.ui.map.RouteResult
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userPrefs: UserPrefs

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var filterAdapter: FilterTagAdapter
    private lateinit var guideStepAdapter: GuideStepAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter

    private var aMap: AMap? = null
    /** 仅在为 true 时调用 MapView 生命周期，避免 onCreate 前 onResume 导致闪退 */
    private var mapViewCreated = false

    // 导航相关
    private var routeNavigator: RouteNavigator? = null
    private var navigationPanelBinding: LayoutNavigationPanelBinding? = null
    private var navigationTarget: Location? = null

    // 定位相关
    private var aMapLocationClient: AMapLocationClient? = null
    private var myLocationMarker: com.amap.api.maps.model.Marker? = null
    private var latestLocation: android.location.Location? = null

    private var latestGcjLatLng: LatLng? = null
    private var routePolyline: Polyline? = null
    private var navTargetForExternal: Location? = null

    // 搜索相关
    private var cardSearchResults: androidx.cardview.widget.CardView? = null
    private var rvSearchResults: RecyclerView? = null

    private var suppressSearchResultsUntilEdit = false


    // BottomSheet 内部视图缓存
    private var sheetTitle: TextView? = null
    private var sheetRating: TextView? = null
    private var sheetHours: TextView? = null
    private var sheetProgress: ContentLoadingProgressBar? = null
    private var sheetRecycler: RecyclerView? = null
    private var sheetEmpty: TextView? = null
    private var sheetCover: ImageView? = null
    private var btnContributeGuide: MaterialButton? = null
    private var btnFavorite: MaterialButton? = null
    private var currentSheetLocation: Location? = null

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            binding.etSearch.setText(text)
            viewModel.submitSearch(text)
        }
    }

    // 导航面板视图缓存
    private var tvRouteDistance: TextView? = null
    private var tvRouteDuration: TextView? = null
    private var tvNextStep: TextView? = null
    private var cardNavigationPanel: CardView? = null

    // 详情弹窗
    private var locationDetailBinding: LayoutLocationDetailBinding? = null
    private var cardLocationDetail: CardView? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            initAndStartAmapLocation()
        }
    }

    // ── Fragment lifecycle ─────────────────────────────────────────

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

        setupBottomSheet()
        setupNavigationPanel()
        setupLocationDetail()
        setupFilterTags()
        setupSearchBar()
        setupSearch()
        setupMyLocationButton()
        setupMenuButton()
        setupWalkNavigationPanel()
        setupViewModelObservers()
        observeMainNav()

        if (viewModel.isPrivacyAgreed) {
            onPrivacyApproved(savedInstanceState)
        } else {
            showPrivacyDialog(savedInstanceState)
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

        stopSystemLocation()
        if (mapViewCreated) {
            binding.mapView.onDestroy()
            mapViewCreated = false
        }

        sheetTitle = null
        sheetRating = null
        sheetHours = null
        sheetProgress = null
        sheetRecycler = null
        sheetEmpty = null
        myLocationMarker = null
        cardSearchResults = null
        rvSearchResults = null
        tvRouteDistance = null
        tvRouteDuration = null
        tvNextStep = null
        cardNavigationPanel = null
        _binding = null
    }

    // 修复崩溃2：NullPointerException - 添加安全检查和try-catch
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            if (mapViewCreated && _binding != null) {
                binding.mapView.onSaveInstanceState(outState)
            }
        } catch (e: Exception) {
            // Fragment可能已被销毁，忽略保存状态失败的异常
        }
    }

    // ── Privacy compliance ──────────────────────────────────────────

    private fun showPrivacyDialog(savedInstanceState: Bundle?) {
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
                initMap(savedInstanceState)
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

    private fun onPrivacyApproved(savedInstanceState: Bundle?) {
        MapsInitializer.updatePrivacyShow(requireContext(), true, true)
        MapsInitializer.updatePrivacyAgree(requireContext(), true)
        ServiceSettings.updatePrivacyShow(requireContext(), true, true)
        ServiceSettings.updatePrivacyAgree(requireContext(), true)
        initMap(savedInstanceState)
        // 若用户同意时 Fragment 已处于 RESUMED，需补调一次 onResume
        if (mapViewCreated && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            binding.mapView.onResume()
        }
    }

    // ── Map initialization ──────────────────────────────────────────

    private fun initMap(savedInstanceState: Bundle?) {
        if (mapViewCreated) return
        binding.mapView.onCreate(savedInstanceState)
        mapViewCreated = true

        binding.mapView.map?.let { map ->
            aMap = map
            viewModel.aMap = map
            configureMap(map)
            setupMarkerClickListener(map)
            initNavigation(map)
            viewModel.loadLocations()
            requestLocationPermissionIfNeeded()
        }
    }

    /**
     * 初始化导航功能
     */
    private fun initNavigation(map: AMap) {
        routeNavigator = RouteNavigator(map)
        routeNavigator?.onRouteCalculated = { result ->
            activity?.runOnUiThread {
                showNavigationPanel(result)
            }
        }
        routeNavigator?.onError = { errorMsg ->
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示导航面板
     */
    private fun showNavigationPanel(result: RouteResult) {
        val panelBinding = navigationPanelBinding ?: return

        // 获取路径信息
        val distance = result.distanceInt
        val duration = result.durationInt / 60

        // 更新面板显示
        panelBinding.tvRouteDistance.text = result.distanceKm
        panelBinding.tvRouteDuration.text = duration.toString()

        // 显示导航提示
        panelBinding.tvNextStep.text = "点击「关闭」结束导航"

        // 显示面板
        panelBinding.cardNavigationPanel.visibility = View.VISIBLE

        // 收起 BottomSheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 隐藏导航面板
     */
    private fun hideNavigationPanel() {
        navigationPanelBinding?.cardNavigationPanel?.visibility = View.GONE
        routeNavigator?.clearRoute()
        navigationTarget = null
    }

    private fun configureMap(map: AMap) {
        // 禁用高德内置定位引擎，避免与系统定位冲突和产生错误提示
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

        // 不再使用 MyLocationStyle，避免高德内部定位引擎自动启动
        // 定位标记由系统 LocationManager 单独管理

        map.mapType = AMap.MAP_TYPE_NORMAL

        val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(

            com.amap.api.maps.model.LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG), 16f

        )
        map.moveCamera(cameraUpdate)
    }

    private fun setupMarkerClickListener(map: AMap) {
        map.setOnMarkerClickListener { marker ->
            val location = marker.`object` as? Location
            if (location != null) {
                // 显示详情弹窗
                showLocationDetailCard(location)
                // 同时更新 ViewModel 的选择状态
                viewModel.selectLocation(location)
            }
            false
        }
    }

    // ── Location permission ─────────────────────────────────────────

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

    // ── 高德定位 ──────────────────────────────────────────────────

    /**
     * 初始化并启动高德定位
     */
    private fun initAndStartAmapLocation() {
        aMapLocationClient = AMapLocationClient(requireContext())

        aMapLocationClient?.onLocationResult = { amapLocation ->
            onAmapLocationReceived(amapLocation)
        }

        // 错误不提示，静默重试
        aMapLocationClient?.onLocationError = { _, _ -> }

        aMapLocationClient?.start()
    }


    private fun onLocationReceived(location: android.location.Location) {
        val gcj = com.example.guet_map.util.CoordinateUtil.wgs84ToGcj02(
            requireContext(),
            location.latitude,
            location.longitude
        )
        latestLocation = location
        latestGcjLatLng = LatLng(gcj.latitude, gcj.longitude)
        val map = aMap ?: return
        val latLng = latestGcjLatLng!!


        // 更新或创建定位标记
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

        // 首次定位成功且未手动移动地图时，自动居中一次
        if (!hasAutoCenteredOnLocation && amapLocation.accuracy < 50) {
            hasAutoCenteredOnLocation = true
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(latLng, 17f)
            map.animateCamera(update, 500, null)
        }
    }

    // ── My Location button ───────────────────────────────────────────

    private fun setupMyLocationButton() {
        binding.fabMyLocation.setOnClickListener {
            centerOnMyLocation()
        }
    }

    private fun centerOnMyLocation() {
        val map = aMap ?: return
        val loc = latestLocation
        if (loc != null) {
            val gcj = com.example.guet_map.util.CoordinateUtil.wgs84ToGcj02(
                requireContext(), loc.latitude, loc.longitude
            )
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(gcj.latitude, gcj.longitude), 17f
            )
            map.moveCamera(update)
        } else {
            // 还没拿到定位，检查权限后启动
            requestLocationPermissionIfNeeded()
            Toast.makeText(requireContext(), "正在获取位置…", Toast.LENGTH_SHORT).show()
        }
    }

    // ── ViewModel observers ─────────────────────────────────────────

    private fun setupViewModelObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 地点列表加载状态
                launch {
                    viewModel.locationsResource.collectLatest { resource ->
                        if (resource is Resource.Error) {
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 地图事件 (展开 BottomSheet 等)
                launch {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is MapEvent.ShowBottomSheet -> showLocationDetail(event.location)
                            is MapEvent.FocusLocation -> {
                                moveMapToLocation(event.location)
                                viewModel.showHighlightMarker(event.location)
                            }
                            is MapEvent.HideBottomSheet -> hideLocationSheet()
                            is MapEvent.DismissSearchUi -> dismissSearchUi()
                        }
                    }
                }

                launch {
                    viewModel.cachedLocations.collectLatest { locations ->
                        if (locations.isNotEmpty()) {
                            viewModel.updateMapMarkersFromCache()
                        }
                    }
                }

                // 图文指引加载状态 → 更新 RecyclerView
                launch {
                    viewModel.guideStepsResource.collectLatest { resource ->
                        when (resource) {
                            is Resource.Loading -> showGuideLoading()
                            is Resource.Success -> showGuideSteps(resource.data)
                            is Resource.Error -> showGuideError(resource.message)
                        }
                    }
                }

                // 选中的地点信息更新
                launch {
                    viewModel.selectedLocation.collect { location ->
                        location?.let { updateSheetHeader(it) }
                    }
                }

                // 搜索结果
                launch {
                    viewModel.searchResults.collectLatest { results ->
                        if (suppressSearchResultsUntilEdit) {
                            cardSearchResults?.visibility = View.GONE
                            return@collectLatest
                        }
                        if (results.isNotEmpty()) {
                            showSearchResults(results)
                        } else {
                            cardSearchResults?.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.favoriteIds.collectLatest { ids ->
                        updateFavoriteButton(ids)
                    }
                }

                launch {
                    viewModel.walkRoute.collectLatest { route ->
                        if (route != null) showWalkRouteOnMap(route) else clearWalkRouteFromMap()
                    }
                }

                launch {
                    viewModel.routeLoading.collectLatest { loading ->
                        binding.root.findViewById<View>(R.id.progressRoute)?.visibility =
                            if (loading) View.VISIBLE else View.GONE
                        if (loading) {
                            binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility =
                                View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.routeError.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupWalkNavigationPanel() {
        binding.root.findViewById<View>(R.id.btnCloseRoute)?.setOnClickListener {
            viewModel.clearWalkRoute()
        }
        binding.root.findViewById<View>(R.id.btnClearRoute)?.setOnClickListener {
            viewModel.clearWalkRoute()
        }
        binding.root.findViewById<View>(R.id.btnOpenAmapNavi)?.setOnClickListener {
            navTargetForExternal?.let { openAmapExternalNavigation(it) }
        }
    }

    private fun showWalkRouteOnMap(route: com.example.guet_map.model.WalkRouteInfo) {
        val map = aMap ?: return
        clearWalkRouteFromMap()
        routePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(route.polyline)
                .width(12f)
                .color(ContextCompat.getColor(requireContext(), R.color.primary))
        )
        val minutes = (route.durationSeconds / 60).coerceAtLeast(1)
        binding.root.findViewById<android.widget.TextView>(R.id.tvRouteSummary)?.text =
            getString(
                R.string.route_summary_format,
                route.targetName,
                route.distanceMeters,
                minutes
            )
        binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility = View.VISIBLE
        binding.root.findViewById<View>(R.id.progressRoute)?.visibility = View.GONE

        val builder = LatLngBounds.builder()
        route.polyline.forEach { builder.include(it) }
        map.animateCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
    }

    private fun clearWalkRouteFromMap() {
        routePolyline?.remove()
        routePolyline = null
        binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility = View.GONE
    }

    private fun showNavigationOptions(location: Location) {
        navTargetForExternal = location
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.nav_choose_title)
            .setItems(
                arrayOf(
                    getString(R.string.nav_campus_walk),
                    getString(R.string.nav_amap_app)
                )
            ) { _, which ->
                when (which) {
                    0 -> startCampusWalkNavigation(location)
                    1 -> openAmapExternalNavigation(location)
                }
            }
            .show()
    }

    private fun startCampusWalkNavigation(location: Location) {
        val start = latestGcjLatLng ?: viewModel.campusCenterLatLng().also {
            Toast.makeText(requireContext(), R.string.route_no_location, Toast.LENGTH_SHORT).show()
        }
        binding.root.findViewById<View>(R.id.cardWalkNav)?.visibility = View.VISIBLE
        binding.root.findViewById<android.widget.TextView>(R.id.tvRouteTitle)?.text =
            getString(R.string.campus_walk_route)
        binding.root.findViewById<android.widget.TextView>(R.id.tvRouteSummary)?.text =
            getString(R.string.route_planning)
        viewModel.planWalkRouteTo(location, start)
    }

    private fun observeMainNav() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainNavViewModel.pendingLocationId.collectLatest { locationId ->
                        locationId ?: return@collectLatest
                        val id = mainNavViewModel.consumePendingLocation() ?: return@collectLatest
                        openLocationOnMap(id)
                    }
                }
                launch {
                    mainNavViewModel.pendingCategory.collectLatest { category ->
                        category ?: return@collectLatest
                        val cat = mainNavViewModel.consumePendingCategory() ?: return@collectLatest
                        if (::filterAdapter.isInitialized) {
                            filterAdapter.setSelectedTag(cat)
                        }
                        viewModel.filterByCategory(cat)
                    }
                }
            }
        }
    }

    private fun openLocationOnMap(locationId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val loc = viewModel.resolveAndSelectLocation(locationId)
            if (loc == null) {
                return@launch
            }
            aMap?.moveCamera(
                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(loc.latitude, loc.longitude), 17f
                )
            )
        }
    }

    private fun setupMenuButton() {
        binding.ivMenu.setOnClickListener {
            mainNavViewModel.requestTab(R.id.nav_login)
        }
        binding.ivAvatar.setOnClickListener {
            mainNavViewModel.requestTab(R.id.nav_login)
        }
    }

    private fun updateFavoriteButton(favoriteIds: Set<String>) {
        val loc = currentSheetLocation ?: return
        val isFav = loc.locationId in favoriteIds
        btnFavorite?.text = if (isFav) "已收藏" else "收藏"
    }

    // ── BottomSheet 状态更新方法 ────────────────────────────────────

    private fun showGuideLoading() {
        sheetProgress?.visibility = View.VISIBLE
        sheetEmpty?.visibility = View.GONE
    }

    private fun showGuideSteps(steps: List<GuideStep>) {
        sheetProgress?.visibility = View.GONE
        val noGuide = steps.isEmpty()
        if (noGuide) {
            sheetEmpty?.visibility = View.VISIBLE
            guideStepAdapter.submitList(emptyList())
            btnContributeGuide?.visibility = View.VISIBLE
        } else {
            sheetEmpty?.visibility = View.GONE
            btnContributeGuide?.visibility = View.GONE
            guideStepAdapter.submitList(steps)
        }
    }

    private fun showGuideError(message: String) {
        sheetProgress?.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateSheetHeader(location: Location) {
        currentSheetLocation = location
        sheetTitle?.text = location.name
        sheetRating?.text = "${location.rating} ★"
        sheetHours?.text = location.openingHours
        if (location.imageUrl.isNotBlank()) {
            sheetCover?.visibility = View.VISIBLE
            sheetCover?.load(location.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
        } else {
            sheetCover?.visibility = View.GONE
        }
        updateFavoriteButton(viewModel.favoriteIds.value)
    }

    private fun showLocationDetail(location: Location) {
        updateSheetHeader(location)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // ── BottomSheet setup ────────────────────────────────────────────

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = dpToPx(48)
        bottomSheetBehavior.isHideable = true

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // 折叠时收起键盘
                    }
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        // 填充 BottomSheet 内容
        val sheetContent = layoutInflater.inflate(R.layout.sheet_guide_detail, null)
        cacheSheetViews(sheetContent)
        configureSheetRecycler()
        configureActionButtons(sheetContent)
        (binding.bottomSheet as ViewGroup).addView(sheetContent)
    }

    private fun cacheSheetViews(root: View) {
        sheetTitle = root.findViewById(R.id.tvSheetTitle)
        sheetRating = root.findViewById(R.id.tvSheetRating)
        sheetHours = root.findViewById(R.id.tvSheetHours)
        sheetProgress = root.findViewById(R.id.progressGuide)
        sheetRecycler = root.findViewById(R.id.rvGuideSteps)
        sheetEmpty = root.findViewById(R.id.tvEmptyGuides)
        sheetCover = root.findViewById(R.id.ivSheetCover)
        btnContributeGuide = root.findViewById(R.id.btnContributeGuide)
        btnFavorite = root.findViewById(R.id.btnFavorite)
    }

    private fun configureSheetRecycler() {
        guideStepAdapter = GuideStepAdapter()
        sheetRecycler?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guideStepAdapter
        }
    }

    /**
     * 设置导航面板
     */
    private fun setupNavigationPanel() {
        // 初始化路径规划客户端
        aMapRouteClient = AMapRouteClient(requireContext()).apply {
            onRouteResult = { result ->
                onRouteResultReceived(result)
            }
            onRouteError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        val navPanelView = layoutInflater.inflate(R.layout.layout_navigation_panel, null)
        navigationPanelBinding = LayoutNavigationPanelBinding.bind(navPanelView)

        // 缓存视图引用
        tvRouteDistance = navigationPanelBinding?.tvRouteDistance
        tvRouteDuration = navigationPanelBinding?.tvRouteDuration
        tvNextStep = navigationPanelBinding?.tvNextStep
        cardNavigationPanel = navigationPanelBinding?.cardNavigationPanel

        // 关闭按钮
        navigationPanelBinding?.btnCloseNavigation?.setOnClickListener {
            hideNavigationPanel()
        }

        // 将导航面板添加到地图容器
        val container = binding.root.findViewById<ViewGroup>(R.id.mapContainer)
        container?.addView(navPanelView)
    }

    /**
     * 设置地点详情弹窗
     */
    private fun setupLocationDetail() {
        val detailView = layoutInflater.inflate(R.layout.layout_location_detail, null)
        locationDetailBinding = LayoutLocationDetailBinding.bind(detailView)
        cardLocationDetail = locationDetailBinding?.cardLocationDetail

        // 关闭按钮
        locationDetailBinding?.btnCloseDetail?.setOnClickListener {
            hideLocationDetail()
        }

        // 导航按钮
        locationDetailBinding?.btnNavigate?.setOnClickListener {
            hideLocationDetail()
            startNavigation()
        }

        // 收藏按钮
        locationDetailBinding?.btnFavorite?.setOnClickListener {
            Toast.makeText(requireContext(), "已加入收藏", Toast.LENGTH_SHORT).show()
        }

        // 将详情弹窗添加到地图容器
        val container = binding.root.findViewById<ViewGroup>(R.id.mapContainer)
        container?.addView(detailView)
    }

    /**
     * 显示地点详情弹窗
     */
    private fun showLocationDetailCard(location: Location) {
        val binding = locationDetailBinding ?: return

        // 填充数据
        binding.tvLocationName.text = location.name
        binding.tvCategory.text = location.category
        binding.tvRating.text = String.format("%.1f", location.rating)
        binding.tvAddress.text = if (location.address.isNotEmpty()) location.address else "暂无地址信息"
        binding.tvOpeningHours.text = if (location.openingHours.isNotEmpty()) location.openingHours else "全天开放"
        binding.tvDescription.text = if (location.description.isNotEmpty()) location.description else "暂无详细描述"

        // 电话（如果有）
        if (location.phone.isNotEmpty()) {
            binding.llPhone.visibility = View.VISIBLE
            binding.tvPhone.text = location.phone
        } else {
            binding.llPhone.visibility = View.GONE
        }

        // 加载图片
        if (location.imageUrl.isNotEmpty()) {
            binding.ivLocationImage.load(location.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_location_placeholder)
                error(R.drawable.ic_location_placeholder)
            }
        } else {
            binding.ivLocationImage.setImageResource(R.drawable.ic_location_placeholder)
        }

        // 显示弹窗
        cardLocationDetail?.visibility = View.VISIBLE

        // 收起 BottomSheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 隐藏地点详情弹窗
     */
    private fun hideLocationDetail() {
        cardLocationDetail?.visibility = View.GONE
    }

    private fun onRouteResultReceived(result: RouteResult) {
        currentRouteResult = result

        // 显示导航面板
        cardNavigationPanel?.visibility = View.VISIBLE

        // 更新距离和时间显示
        tvRouteDistance?.text = result.distanceKm
        tvRouteDuration?.text = (result.durationInt / 60).toString()

        // 显示第一步指示
        if (result.steps.isNotEmpty()) {
            tvNextStep?.text = result.steps.first().instruction
        }

        // 在地图上绘制路径
        routeNavigator?.drawRoute(result)
    }

    private fun configureActionButtons(root: View) {
        root.findViewById<View>(R.id.btnNavigate)?.setOnClickListener {

            val loc = currentSheetLocation ?: return@setOnClickListener
            navTargetForExternal = loc
            startCampusWalkNavigation(loc)
        }
        btnFavorite?.setOnClickListener {
            val loc = currentSheetLocation ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val nowFavorite = viewModel.toggleFavorite(loc)
                Toast.makeText(
                    requireContext(),
                    if (nowFavorite) getString(R.string.favorite_added)
                    else getString(R.string.favorite_removed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        root.findViewById<View>(R.id.btnShare)?.setOnClickListener {
            val loc = currentSheetLocation ?: return@setOnClickListener
            shareLocation(loc)
        }
        btnContributeGuide?.setOnClickListener {
            mainNavViewModel.requestTab(R.id.nav_contribute)
        }
    }


  
     * 开始导航
     * 从当前位置导航到选中的地点
     */
    private fun startNavigation() {
        val target = viewModel.selectedLocation.value
        val currentLocation = latestLocation

        if (target == null) {
            Toast.makeText(requireContext(), "请先选择一个目的地", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLocation == null) {
            Toast.makeText(requireContext(), "正在获取您的位置信息...", Toast.LENGTH_SHORT).show()
            requestLocationPermissionIfNeeded()
            return
        }

        // 保存导航目标
        navigationTarget = target

        // 使用高德路径规划 API
        aMapRouteClient?.searchWalkingRoute(
            originLat = currentLocation.latitude,
            originLng = currentLocation.longitude,
            destLat = target.latitude,
            destLng = target.longitude
        )
        Toast.makeText(requireContext(), "正在规划路线...", Toast.LENGTH_SHORT).show()
    } // 注意：帮你补上了这个缺失的右括号

    // 修复崩溃1：ActivityNotFoundException - 改进导航Intent处理
    private fun openAmapExternalNavigation(location: Location) {
        val target = viewModel.cachedLocations.value
            .find { it.locationId == location.locationId } ?: location
        val pm = requireContext().packageManager

        try {
            // 方案1：尝试高德地图App
            val start = latestGcjLatLng
            val uriBuilder = StringBuilder("androidamap://route/plan/?")
            uriBuilder.append("dlat=${target.latitude}&dlon=${target.longitude}")
            uriBuilder.append("&dname=${Uri.encode(target.name)}")
            uriBuilder.append("&dev=0&t=2")
            if (start != null) {
                uriBuilder.append("&slat=${start.latitude}&slon=${start.longitude}")
                uriBuilder.append("&sname=${Uri.encode("我的位置")}")
            }

            val amapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
                setPackage("com.autonavi.minimap")
            }

            if (amapIntent.resolveActivity(pm) != null) {
                startActivity(amapIntent)
                return
            }

            // 方案2：尝试通用geo协议（Google Maps等）
            val geoUri = Uri.parse(
                "geo:${target.latitude},${target.longitude}?q=${Uri.encode(target.name)}"
            )
            val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)

            if (geoIntent.resolveActivity(pm) != null) {
                startActivity(Intent.createChooser(geoIntent, getString(R.string.nav_amap_app)))
                return
            }

            // 方案3：如果都没有，打开网页版地图
            val webUri = Uri.parse(
                "https://uri.amap.com/marker?position=${target.longitude},${target.latitude}&name=${Uri.encode(target.name)}"
            )
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)

            if (webIntent.resolveActivity(pm) != null) {
                Toast.makeText(
                    requireContext(),
                    "未安装地图应用，将使用浏览器打开网页地图",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(webIntent)
            } else {
                // 最后的备选方案：复制坐标到剪贴板
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "坐标",
                    "${target.name}: ${target.latitude}, ${target.longitude}"
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    requireContext(),
                    "未找到可用导航应用，坐标已复制到剪贴板",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "无法打开导航：${e.localizedMessage ?: "未知错误"}",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    // ── Filter tags ─────────────────────────────────────────────────

    private fun setupFilterTags() {
        val filterTags = listOf(
            "食堂", "教室", "咖啡", "图书馆",
            "宿舍", "校门", "商店", "运动场"
        )
        filterAdapter = FilterTagAdapter(filterTags) { tag ->
            viewModel.filterByCategory(tag)
        }
        binding.rvFilterTags.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilterTags.adapter = filterAdapter
    }

    // ── Search bar ──────────────────────────────────────────────────

    private fun setupSearchBar() {
        // 初始化高德搜索客户端
        aMapSearchClient = AMapSearchClient(requireContext()).apply {
            onPoiSearchResult = { results ->
                showPoiSearchResults(results)
            }
            onSearchError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            private var searchRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val query = s?.toString().orEmpty()
                suppressSearchResultsUntilEdit = false
                viewModel.setSearchQuery(query)
                if (query.isBlank()) {
                    cardSearchResults?.visibility = View.GONE
                }

            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            ) {
                val q = binding.etSearch.text?.toString().orEmpty()
                if (q.isNotBlank()) {
                    viewModel.submitSearch(q)
                }
                true
            } else {
                false
            }
        }
        binding.ivVoice.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun startVoiceSearch() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchVoiceRecognizer()
    }

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchVoiceRecognizer()
        else Toast.makeText(requireContext(), "需要麦克风权限才能使用语音搜索", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "当前设备不支持语音识别", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        cardSearchResults = binding.cardSearchResults
        rvSearchResults = binding.rvSearchResults

        searchResultAdapter = SearchResultAdapter { location ->

            binding.etSearch.setText(location.name)
            binding.etSearch.setSelection(location.name.length)
            viewModel.pickFromSearch(location)

        }
        rvSearchResults?.adapter = searchResultAdapter
    }

    private fun showPoiSearchResults(results: List<PoiSearchResult>) {
        // 清除之前的 POI 标记
        clearPoiMarkers()

        if (results.isEmpty()) {
            Toast.makeText(requireContext(), "未找到相关地点", Toast.LENGTH_SHORT).show()
            return
        }

        // 在地图上添加标记
        results.forEachIndexed { index, poi ->
            val marker = aMap?.addMarker(
                MarkerOptions()
                    .position(LatLng(poi.latitude, poi.longitude))
                    .title(poi.name)
                    .snippet(poi.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker?.let { poiMarkers.add(it) }
        }

        // 自动居中到第一个结果
        val firstResult = results.first()
        val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
            LatLng(firstResult.latitude, firstResult.longitude), 16f
        )
        aMap?.animateCamera(update)

        // 显示搜索结果列表
        cardSearchResults?.visibility = View.VISIBLE
        val locations = results.map { poi ->
            Location(
                locationId = poi.id,
                name = poi.name,
                latitude = poi.latitude,
                longitude = poi.longitude,
                category = "搜索结果",
                rating = 0f,
                openingHours = "",
                imageUrl = "",
                hasGuide = false
            )
        }
        searchResultAdapter.submitList(locations)

        // 设置 POI 点击事件
        aMap?.setOnMarkerClickListener { marker ->
            if (poiMarkers.contains(marker)) {
                val index = poiMarkers.indexOf(marker)
                if (index >= 0 && index < results.size) {
                    val poi = results[index]
                    Toast.makeText(requireContext(), "${poi.name}\n${poi.address}", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun clearPoiMarkers() {
        poiMarkers.forEach { it.remove() }
        poiMarkers.clear()
    }

    private fun showSearchResults(results: List<Location>) {
        cardSearchResults?.visibility = View.VISIBLE
        searchResultAdapter.submitList(results)
        // 自动居中到第一个搜索结果
        if (results.isNotEmpty()) {
            val firstResult = results.first()
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(firstResult.latitude, firstResult.longitude), 17f
            )
            aMap?.animateCamera(update)
        }
    }

    private fun moveMapToLocation(location: Location) {
        val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude), 17f
        )
        aMap?.animateCamera(update)
    }

    private fun dismissSearchUi() {
        cardSearchResults?.visibility = View.GONE

        suppressSearchResultsUntilEdit = true

        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun hideLocationSheet() {
        if (::bottomSheetBehavior.isInitialized) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    // ── Utility ─────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}