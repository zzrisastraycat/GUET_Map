package com.example.guet_map.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.maps.AMap
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentMapBinding
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var filterAdapter: FilterTagAdapter
    private lateinit var guideStepAdapter: GuideStepAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter

    private var aMap: AMap? = null

    private lateinit var locationManager: LocationManager
    private var myLocationMarker: com.amap.api.maps.model.Marker? = null
    private var latestLocation: android.location.Location? = null

    // 搜索相关
    private var cardSearchResults: androidx.cardview.widget.CardView? = null
    private var rvSearchResults: RecyclerView? = null

    // BottomSheet 内部视图缓存
    private var sheetTitle: TextView? = null
    private var sheetRating: TextView? = null
    private var sheetHours: TextView? = null
    private var sheetProgress: ContentLoadingProgressBar? = null
    private var sheetRecycler: RecyclerView? = null
    private var sheetEmpty: TextView? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            startSystemLocation()
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

        locationManager = requireContext().getSystemService(LocationManager::class.java)

        setupBottomSheet()
        setupFilterTags()
        setupSearchBar()
        setupSearch()
        setupMyLocationButton()
        setupViewModelObservers()

        if (viewModel.isPrivacyAgreed) {
            onPrivacyApproved(savedInstanceState)
        } else {
            showPrivacyDialog(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSystemLocation()
        binding.mapView.onDestroy()
        sheetTitle = null
        sheetRating = null
        sheetHours = null
        sheetProgress = null
        sheetRecycler = null
        sheetEmpty = null
        myLocationMarker = null
        cardSearchResults = null
        rvSearchResults = null
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
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
                // 隐私 API 已在 mapView.onCreate() 之前调用，直接初始化即可
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
        initMap(savedInstanceState)
    }

    // ── Map initialization ──────────────────────────────────────────

    private fun initMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.map?.let { map ->
            aMap = map
            viewModel.aMap = map
            configureMap(map)
            setupMarkerClickListener(map)
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

        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        myLocationStyle.interval(2000)
        myLocationStyle.radiusFillColor(
            ContextCompat.getColor(requireContext(), R.color.location_fill)
        )
        myLocationStyle.strokeColor(
            ContextCompat.getColor(requireContext(), R.color.location_stroke)
        )
        myLocationStyle.strokeWidth(2f)
        map.myLocationStyle = myLocationStyle

        map.mapType = AMap.MAP_TYPE_NORMAL

        val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
            com.amap.api.maps.model.LatLng(25.2851, 110.4131), 16f
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

    // ── Location permission ─────────────────────────────────────────

    private fun requestLocationPermissionIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            startSystemLocation()
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

    // ── 系统定位 (LocationManager，不依赖高德定位引擎) ──────────

    private val locationListener = android.location.LocationListener { location ->
        onLocationReceived(location)
    }

    private fun startSystemLocation() {
        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                locationManager.requestLocationUpdates(
                    provider, 2000L, 5f, locationListener
                )
            }
        } catch (e: SecurityException) {
            // 权限不足
        }
    }

    private fun onLocationReceived(location: android.location.Location) {
        latestLocation = location
        val map = aMap ?: return
        val latLng = LatLng(location.latitude, location.longitude)

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
    }

    private fun stopSystemLocation() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: Exception) {}
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
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(loc.latitude, loc.longitude), 17f
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
                        if (results.isNotEmpty()) {
                            showSearchResults(results)
                        } else {
                            cardSearchResults?.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    // ── BottomSheet 状态更新方法 ────────────────────────────────────

    private fun showGuideLoading() {
        sheetProgress?.visibility = View.VISIBLE
        sheetEmpty?.visibility = View.GONE
    }

    private fun showGuideSteps(steps: List<GuideStep>) {
        sheetProgress?.visibility = View.GONE
        if (steps.isEmpty()) {
            sheetEmpty?.visibility = View.VISIBLE
            guideStepAdapter.submitList(emptyList())
        } else {
            sheetEmpty?.visibility = View.GONE
            guideStepAdapter.submitList(steps)
        }
    }

    private fun showGuideError(message: String) {
        sheetProgress?.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateSheetHeader(location: Location) {
        sheetTitle?.text = location.name
        sheetRating?.text = "${location.rating} ★"
        sheetHours?.text = location.openingHours
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
    }

    private fun configureSheetRecycler() {
        guideStepAdapter = GuideStepAdapter()
        sheetRecycler?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guideStepAdapter
        }
    }

    private fun configureActionButtons(root: View) {
        root.findViewById<View>(R.id.btnNavigate)?.setOnClickListener {
            Toast.makeText(requireContext(), "导航功能将在后续版本开放", Toast.LENGTH_SHORT).show()
        }
        root.findViewById<View>(R.id.btnFavorite)?.setOnClickListener {
            Toast.makeText(requireContext(), "已加入收藏", Toast.LENGTH_SHORT).show()
        }
        root.findViewById<View>(R.id.btnShare)?.setOnClickListener {
            Toast.makeText(requireContext(), "分享功能将在后续版本开放", Toast.LENGTH_SHORT).show()
        }
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
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.ivVoice.setOnClickListener {
            Toast.makeText(requireContext(), "语音搜索将在后续版本开放", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        cardSearchResults = binding.cardSearchResults
        rvSearchResults = binding.rvSearchResults

        searchResultAdapter = SearchResultAdapter { location ->
            // 选中结果：移动相机 → 展开详情 → 清空搜索
            hideSearchResults()
            val update = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 17f
            )
            aMap?.moveCamera(update)
            viewModel.selectLocation(location)
        }
        rvSearchResults?.adapter = searchResultAdapter
    }

    private fun showSearchResults(results: List<Location>) {
        cardSearchResults?.visibility = View.VISIBLE
        searchResultAdapter.submitList(results)
    }

    private fun hideSearchResults() {
        cardSearchResults?.visibility = View.GONE
        binding.etSearch.text?.clear()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    // ── Utility ─────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
