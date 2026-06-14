package com.example.guet_map.ui.map.component

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentMapBinding
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Location
import com.example.guet_map.ui.map.GuideStepAdapter

/**
 * BottomSheet 详情组件
 * 负责：地点详情的 BottomSheet 展示、图文指引列表
 */
class LocationBottomSheetComponent(
    private val binding: FragmentMapBinding
) {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var guideStepAdapter: GuideStepAdapter

    // 视图缓存
    private var sheetTitle: TextView? = null
    private var sheetRating: TextView? = null
    private var sheetHours: TextView? = null
    private var sheetProgress: ContentLoadingProgressBar? = null
    private var sheetRecycler: RecyclerView? = null
    private var sheetEmpty: TextView? = null
    private var sheetCover: ImageView? = null
    private var btnContributeGuide: MaterialButton? = null
    private var btnFavorite: MaterialButton? = null
    private var currentLocation: Location? = null

    var onNavigate: ((Location) -> Unit)? = null
    var onFavorite: ((Location) -> Unit)? = null
    var onShare: ((Location) -> Unit)? = null
    var onContributeGuide: (() -> Unit)? = null

    init {
        setupBottomSheet()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = dpToPx(48)
        bottomSheetBehavior.isHideable = true

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        currentLocation = null
                    }
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        // 填充 BottomSheet 内容
        val sheetContent = LayoutInflater.from(binding.root.context)
            .inflate(R.layout.sheet_guide_detail, binding.bottomSheet, false)
        cacheSheetViews(sheetContent)
        configureSheetRecycler()
        configureActionButtons(sheetContent)
        binding.bottomSheet.addView(sheetContent)
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
            layoutManager = LinearLayoutManager(context)
            adapter = guideStepAdapter
        }
    }

    private fun configureActionButtons(root: View) {
        root.findViewById<View>(R.id.btnNavigate)?.setOnClickListener {
            currentLocation?.let { loc ->
                onNavigate?.invoke(loc)
            }
        }

        btnFavorite?.setOnClickListener {
            currentLocation?.let { loc ->
                onFavorite?.invoke(loc)
            }
        }

        root.findViewById<View>(R.id.btnShare)?.setOnClickListener {
            currentLocation?.let { loc ->
                onShare?.invoke(loc)
            }
        }

        btnContributeGuide?.setOnClickListener {
            onContributeGuide?.invoke()
        }
    }

    /**
     * 显示地点详情
     */
    fun show(location: Location, isFavorite: Boolean = false) {
        currentLocation = location

        sheetTitle?.text = location.name
        sheetRating?.text = "${location.rating} ★"
        sheetHours?.text = location.openingHours.ifEmpty { "全天开放" }

        if (location.imageUrl.isNotBlank()) {
            sheetCover?.isVisible = true
            sheetCover?.let { iv ->
                iv.load(location.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            }
        } else {
            sheetCover?.isVisible = false
        }

        updateFavoriteState(isFavorite)
        expand()
    }

    /**
     * 更新收藏状态
     */
    fun updateFavoriteState(isFavorite: Boolean) {
        btnFavorite?.text = if (isFavorite) "已收藏" else "收藏"
    }

    /**
     * 显示指引加载中
     */
    fun showGuideLoading() {
        sheetProgress?.isVisible = true
        sheetEmpty?.isVisible = false
    }

    /**
     * 显示指引列表
     */
    fun showGuideSteps(steps: List<GuideStep>) {
        sheetProgress?.isVisible = false
        if (steps.isEmpty()) {
            sheetEmpty?.isVisible = true
            guideStepAdapter.submitList(emptyList())
            btnContributeGuide?.isVisible = true
        } else {
            sheetEmpty?.isVisible = false
            btnContributeGuide?.isVisible = false
            guideStepAdapter.submitList(steps)
        }
    }

    /**
     * 显示指引错误
     */
    fun showGuideError(message: String) {
        sheetProgress?.isVisible = false
        sheetEmpty?.isVisible = true
        sheetEmpty?.text = message
    }

    /**
     * 展开 BottomSheet
     */
    fun expand() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * 收起 BottomSheet
     */
    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * 隐藏 BottomSheet
     */
    fun hide() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        currentLocation = null
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean =
        bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN

    /**
     * 获取当前选中的地点
     */
    fun getCurrentLocation(): Location? = currentLocation

    private fun dpToPx(dp: Int): Int {
        return (dp * binding.root.context.resources.displayMetrics.density).toInt()
    }
}
