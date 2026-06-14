package com.example.guet_map.ui.map.component

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.guet_map.R
import com.example.guet_map.databinding.LayoutLocationDetailBinding
import com.example.guet_map.model.Location

/**
 * 地点详情卡片组件
 * 负责：地点详情展示、收藏、分享、导航
 */
class LocationDetailCardComponent(
    private val context: Context,
    private val parent: ViewGroup
) {
    private var binding: LayoutLocationDetailBinding? = null
    private var currentLocation: Location? = null

    var onClose: (() -> Unit)? = null
    var onNavigate: ((Location) -> Unit)? = null
    var onFavorite: ((Location) -> Unit)? = null
    var onShare: ((Location) -> Unit)? = null

    init {
        inflate()
    }

    private fun inflate() {
        val detailView = LayoutLocationDetailBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        binding = detailView
        parent.addView(detailView.root)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding?.btnCloseDetail?.setOnClickListener {
            hide()
            onClose?.invoke()
        }

        binding?.btnNavigate?.setOnClickListener {
            currentLocation?.let { loc ->
                hide()
                onNavigate?.invoke(loc)
            }
        }

        binding?.btnFavorite?.setOnClickListener {
            currentLocation?.let { loc ->
                onFavorite?.invoke(loc)
            }
        }
    }

    /**
     * 显示地点详情
     */
    fun show(location: Location) {
        currentLocation = location

        binding?.apply {
            tvLocationName.text = location.name
            tvCategory.text = location.category
            tvRating.text = String.format("%.1f", location.rating)
            tvAddress.text = if (location.address.isNotEmpty()) location.address else "暂无地址信息"
            tvOpeningHours.text = if (location.openingHours.isNotEmpty()) location.openingHours else "全天开放"
            tvDescription.text = if (location.description.isNotEmpty()) location.description else "暂无详细描述"

            // 电话
            if (location.phone.isNotEmpty()) {
                llPhone.isVisible = true
                tvPhone.text = location.phone
                tvPhone.setOnClickListener {
                    dialPhone(location.phone)
                }
            } else {
                llPhone.isVisible = false
            }

            // 图片
            if (location.imageUrl.isNotEmpty()) {
                ivLocationImage.load(location.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_location_placeholder)
                    error(R.drawable.ic_location_placeholder)
                    transformations(RoundedCornersTransformation(16f))
                }
            } else {
                ivLocationImage.setImageResource(R.drawable.ic_location_placeholder)
            }

            cardLocationDetail.isVisible = true
        }
    }

    /**
     * 更新收藏状态
     */
    fun updateFavoriteState(isFavorite: Boolean) {
        binding?.btnFavorite?.text = if (isFavorite) "已收藏" else "收藏"
    }

    /**
     * 隐藏详情卡片
     */
    fun hide() {
        binding?.cardLocationDetail?.isVisible = false
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean =
        binding?.cardLocationDetail?.isVisible == true

    /**
     * 获取当前位置
     */
    fun getCurrentLocation(): Location? = currentLocation

    private fun dialPhone(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$phone")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法拨打电话", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLocation(location: Location) {
        val text = buildString {
            appendLine(location.name)
            appendLine("分类：${location.category}")
            if (location.address.isNotEmpty()) {
                appendLine("地址：${location.address}")
            }
            appendLine("坐标：${location.latitude}, ${location.longitude}")
            append("来自 GUET Map 校园导航")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享地点"))
    }

    /**
     * 释放资源
     */
    fun destroy() {
        parent.removeView(binding?.root)
        binding = null
    }
}
