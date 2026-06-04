package com.example.guet_map.ui.map

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener

/**
 * 高德地图定位客户端
 * 使用高德专业定位 SDK，提供室内外精准定位
 */
class AMapLocationClient(context: Context) {

    private var locationClient: AMapLocationClient? = null
    private var isStarted = false

    var onLocationResult: ((AMapLocation) -> Unit)? = null
    var onLocationError: ((Int, String) -> Unit)? = null

    init {
        try {
            locationClient = AMapLocationClient(context.applicationContext)
            locationClient?.setLocationListener(LocationListener())
        } catch (e: Exception) {
            onLocationError?.invoke(-1, "定位客户端初始化失败: ${e.message}")
        }
    }

    /**
     * 开始高精度定位
     * 优先 GPS，室内自动切换网络定位
     */
    fun start() {
        if (isStarted) return

        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            interval = 2000L
            isNeedAddress = true
            isMockEnable = false
            isOnceLocation = false
        }

        locationClient?.setLocationOption(option)
        try {
            locationClient?.startLocation()
            isStarted = true
        } catch (e: Exception) {
            onLocationError?.invoke(-1, "启动定位失败: ${e.message}")
        }
    }

    /**
     * 停止定位
     */
    fun stop() {
        if (!isStarted) return
        try {
            locationClient?.stopLocation()
            isStarted = false
        } catch (_: Exception) {}
    }

    /**
     * 销毁客户端
     */
    fun destroy() {
        stop()
        try {
            locationClient?.onDestroy()
        } catch (_: Exception) {}
        locationClient = null
    }

    private inner class LocationListener : AMapLocationListener {
        override fun onLocationChanged(amapLocation: AMapLocation?) {
            if (amapLocation == null) {
                onLocationError?.invoke(-1, "定位结果为空")
                return
            }

            val errorCode = amapLocation.errorCode
            if (errorCode == 0) {
                onLocationResult?.invoke(amapLocation)
            }
            // 错误不提示，静默重试
        }
    }

    companion object {
        fun isLocationSuccess(amapLocation: AMapLocation?): Boolean {
            return amapLocation?.errorCode == 0
        }

        fun toStandardLocation(amapLocation: AMapLocation): android.location.Location {
            return android.location.Location(amapLocation.provider ?: "amap").apply {
                latitude = amapLocation.latitude
                longitude = amapLocation.longitude
                accuracy = amapLocation.accuracy
                time = amapLocation.time
                if (amapLocation.altitude != 0.0) {
                    altitude = amapLocation.altitude
                }
                if (amapLocation.bearing != 0f) {
                    bearing = amapLocation.bearing
                }
                if (amapLocation.speed != 0f) {
                    speed = amapLocation.speed
                }
            }
        }
    }
}
