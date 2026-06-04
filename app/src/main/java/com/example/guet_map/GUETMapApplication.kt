package com.example.guet_map

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.example.guet_map.util.LocalNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GUETMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 高德 SDK v10+：initialize 必须在任何其他 AMap API 之前调用
        // 否则 MapView 创建时 Native 层会闪退
        try {
            MapsInitializer.initialize(this)
            android.util.Log.i("GUETMapApp", "MapsInitializer.initialize succeeded")
        } catch (e: Exception) {
            android.util.Log.e("GUETMapApp", "MapsInitializer.initialize failed", e)
        }

        // 隐私合规：先设置展示状态，再设置同意状态
        try {
            MapsInitializer.updatePrivacyShow(this, true, true)
            ServiceSettings.updatePrivacyShow(this, true, true)
            android.util.Log.i("GUETMapApp", "updatePrivacyShow succeeded")
        } catch (e: Exception) {
            android.util.Log.e("GUETMapApp", "updatePrivacyShow failed", e)
        }

        try {
            val prefs = getSharedPreferences("map_privacy", MODE_PRIVATE)
            val agreed = prefs.getBoolean("privacy_agreed", false)
            MapsInitializer.updatePrivacyAgree(this, agreed)
            ServiceSettings.updatePrivacyAgree(this, agreed)
            android.util.Log.i("GUETMapApp", "updatePrivacyAgree succeeded, agreed=$agreed")
        } catch (e: Exception) {
            android.util.Log.e("GUETMapApp", "updatePrivacyAgree failed", e)
        }

        try {
            LocalNotificationHelper.ensureChannel(this)
        } catch (e: Exception) {
            android.util.Log.e("GUETMapApp", "ensureChannel failed", e)
        }

        // 设置默认异常处理器，捕获未处理异常
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("GUETMapApp", "UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}