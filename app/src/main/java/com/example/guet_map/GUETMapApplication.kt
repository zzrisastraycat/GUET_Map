package com.example.guet_map

import android.app.Application
import com.amap.api.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GUETMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 如果用户已经同意过隐私政策，必须在此处提前调用，
        // 确保 MapView 在 inflate 之前 SDK 已拿到隐私授权，否则黑屏。
        val prefs = getSharedPreferences("map_privacy", MODE_PRIVATE)
        if (prefs.getBoolean("privacy_agreed", false)) {
            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, true)
        }
    }
}
