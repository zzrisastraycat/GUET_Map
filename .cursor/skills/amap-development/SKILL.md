---
name: amap-android
description: 高德地图 Android SDK 开发指南。用于集成高德地图、定位、路径规划功能。涉及 AMap、LocationClient、RouteSearch。
---

# 高德地图 Android SDK 开发

## 1. 依赖配置

### Gradle 依赖
```kotlin
dependencies {
    // 地图 3D SDK（已包含定位模块）
    implementation("com.amap.api:map3d:latest")

    // 独立定位 SDK（可选，如需更精确控制）
    implementation("com.amap.api:location:latest")

    // 路径搜索 SDK（步行/驾车/公交）
    implementation("com.amap.api:search:latest")
}
```

### AndroidManifest 配置
```xml
<!-- 权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- 高德 Key -->
<meta-data
    android:name="com.amap.api.v2.apikey"
    android:value="YOUR_API_KEY" />
```

### NDK 配置
```kotlin
android {
    ndk {
        abiFilters += listOf("armeabi-v7a", "arm64-v8a")
    }
}
```

## 2. 地图初始化

```kotlin
// Fragment 中使用
class MapFragment : Fragment() {

    private lateinit var aMap: AMap
    private lateinit var mapView: MapView

    override fun onCreateView(...): View {
        mapView = MapView(context)
        return mapView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aMap = mapView.map
    }

    // 生命周期必须调用
    override fun onResume() = mapView.onResume()
    override fun onPause() = mapView.onPause()
    override fun onSaveInstanceState(outState: Bundle) = mapView.onSaveInstanceState(outState)
    override fun onDestroy() = mapView.onDestroy()
}
```

## 3. 地图配置

```kotlin
private fun configureMap(map: AMap) {
    // 禁用默认定位按钮
    map.uiSettings.isMyLocationButtonEnabled = false

    // 配置定位样式
    val style = MyLocationStyle().apply {
        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        interval(2000)
        radiusFillColor(Color.argb(50, 0, 123, 255))
        strokeColor(Color.rgb(0, 123, 255))
        strokeWidth(2f)
    }
    map.myLocationStyle = style
    map.isMyLocationEnabled = true

    // 移动到初始位置
    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
        LatLng(25.2851, 110.4131), 16f
    )
    map.moveCamera(cameraUpdate)
}
```

## 4. 定位功能

### 系统定位（使用 LocationManager）
```kotlin
private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

private val locationListener = LocationListener { location ->
    val latLng = LatLng(location.latitude, location.longitude)
    // 更新地图标记
}

private fun startLocation() {
    try {
        val providers = locationManager.getProviders(true)
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(
                provider, 2000L, 5f, locationListener
            )
        }
    } catch (e: SecurityException) {
        // 处理权限不足
    }
}
```

### 高德定位 SDK
```kotlin
private val locationClient = AMapLocationClient(context)

locationClient.setLocationListener { aMapLocation ->
    if (aMapLocation.errorCode == 0) {
        val latLng = LatLng(
            aMapLocation.latitude,
            aMapLocation.longitude
        )
    }
}

locationClient.startLocation()
```

## 5. 相机操作

```kotlin
// 移动到指定位置
val update = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
aMap.moveCamera(update)

// 带动画移动
val update = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
aMap.animateCamera(update, 500, null)

// 缩放
val update = CameraUpdateFactory.zoomIn()
aMap.animateCamera(update)

// 根据边界移动
val bounds = LatLngBounds.Builder()
    .include(startLatLng)
    .include(endLatLng)
    .build()
val update = CameraUpdateFactory.newLatLngBounds(bounds, 100)
aMap.moveCamera(update)
```

## 6. Marker 管理

```kotlin
// 添加标记
val marker = aMap.addMarker(
    MarkerOptions()
        .position(latLng)
        .title("标题")
        .snippet("描述")
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
)

// 绑定数据
marker.`object` = locationData

// 点击监听
aMap.setOnMarkerClickListener { marker ->
    val data = marker.`object` as LocationData
    showDetail(data)
    true
}

// 清除所有标记
markers.forEach { it.remove() }
```

## 7. 路径规划（待实现）

### 步行路径搜索
```kotlin
private fun searchWalkingRoute(start: LatLng, end: LatLng) {
    val from = LatLonPoint(start.latitude, start.longitude)
    val to = LatLonPoint(end.latitude, end.longitude)

    val query = WalkingRouteQuery(from, to, WalkingRouteSearch.WALKING_DEFAULT)
    val search = WalkingRouteSearch(context)
    search.setWalkingRouteSearchListener(listener)
    search.calculateRoute(query)
}

private val listener = object : WalkingRouteSearchListener {
    override fun onWalkingRouteSearched(result: WalkingRouteResult?, errorCode: Int) {
        if (errorCode == AMapException.CODE_OK && result != null) {
            val route = result.routes[0]
            val path = route.walkingParcel

            // 绘制路径线
            val polyline = aMap.addPolyline(
                PolylineOptions()
                    .add(*path.toTypedArray())
                    .color(Color.BLUE)
                    .width(10f)
            )
        }
    }
}
```

### 路径绑路（导航跟随）
- 将用户位置投影到最近路径点
- 沿路径方向更新引导标记
- 转弯时触发提醒

## 8. 常见问题

| 问题 | 解决方案 |
|------|----------|
| 地图不显示 | 检查 API Key 是否正确配置 |
| 定位不准 | 确保 GPS 开启，尝试切换定位模式 |
| 路径规划失败 | 检查网络连接，确认起终点可到达 |
| 标记图标不显示 | 检查 drawable 资源是否正确 |

## 9. 性能优化

- 避免频繁调用 `map.invalidate()`
- 复用 Marker 对象
- 路径规划在后台线程执行
- 缩放级别与数据密度匹配
