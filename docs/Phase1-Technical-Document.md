# GUET_Map 项目第一阶段技术文档

> **项目名称**: 桂林电子科技大学校园地图
> **文档版本**: v1.0.0
> **编写日期**: 2026-06-02
> **文档阶段**: 第一阶段

---

## 一、项目概述

### 1.1 项目背景

GUET_Map 是一款面向桂林电子科技大学师生的校园地图应用，旨在为用户提供便捷的校园地点搜索、定位以及导航服务。

### 1.2 技术栈

| 类别 | 技术选型 |
|------|---------|
| **地图 SDK** | 高德地图 3D SDK (`com.amap.api:map3d`) |
| **架构模式** | MVVM + Clean Architecture |
| **依赖注入** | Hilt |
| **本地数据库** | Room |
| **网络层** | Retrofit + OkHttp |
| **异步处理** | Kotlin Coroutines + Flow |
| **UI 组件** | Material Design Components |

---

## 二、第一阶段功能实现

### 功能一：搜索地点并居中显示

#### 2.1.1 功能描述

用户在搜索框输入关键词后，按回车键或点击搜索结果，地图会自动将目标地点移动到屏幕中央，并以合适的缩放级别展示。

#### 2.1.2 实现原理

**搜索流程**:

```
用户输入 → TextWatcher 监听 → ViewModel 更新搜索关键词
    → Flow 组合过滤 → 返回匹配结果列表 → UI 展示搜索结果
    → 用户点击结果 → 地图移动到目标位置
```

**核心代码实现**:

```kotlin
// MapViewModel.kt - 搜索关键词与搜索结果绑定
private val _searchQuery = MutableStateFlow("")
val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

val searchResults: StateFlow<List<Location>> = _searchQuery
    .combine(cachedLocations) { query, locations ->
        if (query.isBlank()) emptyList()
        else locations.filter { it.name.contains(query, ignoreCase = true) }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

fun setSearchQuery(query: String) {
    _searchQuery.value = query
}
```

```kotlin
// MapFragment.kt - 搜索结果点击后地图移动到目标位置
private fun setupSearch() {
    searchResultAdapter = SearchResultAdapter { location ->
        hideSearchResults()
        // 创建相机更新：将目标位置移动到屏幕中央，缩放级别 17
        val update = CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude), 17f
        )
        aMap?.moveCamera(update)
        viewModel.selectLocation(location)
    }
}
```

#### 2.1.3 关键文件

| 文件 | 职责 |
|------|------|
| `ui/map/MapFragment.kt` | 搜索 UI 绑定与地图交互 |
| `ui/map/MapViewModel.kt` | 搜索逻辑与状态管理 |
| `ui/map/SearchResultAdapter.kt` | 搜索结果列表适配器 |

#### 2.1.4 技术要点

- **模糊匹配**: 使用 `String.contains()` 实现不区分大小写的模糊搜索
- **Flow 响应式**: 利用 Kotlin Flow 的 `combine` 操作符实现搜索关键词与数据源的响应式组合
- **相机动画**: 使用 `CameraUpdateFactory.newLatLngZoom()` 创建平滑的地图移动动画
- **缩放级别**: 搜索结果使用 17 级缩放，确保地点清晰可见

---

### 功能二：获取当前手机定位

#### 2.2.1 功能描述

应用能够获取用户当前的 GPS 位置，并在地图上显示一个定位标记。用户点击"我的位置"按钮后，地图会自动移动到当前位置。

#### 2.2.2 实现原理

**定位流程**:

```
权限检查 → 请求定位权限 → 启动系统 LocationManager
    → 获取位置更新 → 创建/更新定位标记 → 显示当前位置
```

**核心代码实现**:

```kotlin
// MapFragment.kt - 系统定位实现（使用原生 LocationManager）
private val locationListener = LocationListener { location ->
    onLocationReceived(location)
}

private fun startSystemLocation() {
    try {
        // 获取所有可用的定位provider（GPS、网络等）
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            locationManager.requestLocationUpdates(
                provider,      // 定位方式
                2000L,         // 更新间隔 2 秒
                5f,            // 移动距离阈值 5 米
                locationListener
            )
        }
    } catch (e: SecurityException) {
        // 权限不足处理
    }
}
```

```kotlin
// 位置接收与标记更新
private fun onLocationReceived(location: Location) {
    latestLocation = location
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
```

```kotlin
// "我的位置"按钮点击处理
private fun centerOnMyLocation() {
    val loc = latestLocation
    if (loc != null) {
        val update = CameraUpdateFactory.newLatLngZoom(
            LatLng(loc.latitude, loc.longitude), 17f
        )
        map.moveCamera(update)
    } else {
        requestLocationPermissionIfNeeded()
        Toast.makeText(context, "正在获取位置…", Toast.LENGTH_SHORT).show()
    }
}
```

#### 2.2.3 权限配置

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

#### 2.2.4 关键文件

| 文件 | 职责 |
|------|------|
| `ui/map/MapFragment.kt` | 定位功能实现与权限处理 |

#### 2.2.5 技术要点

- **多源定位**: 遍历所有可用的定位提供者（GPS、网络），确保在不同环境下都能定位
- **定位标记**: 使用自定义图标 `ic_my_location` 显示当前位置
- **实时更新**: 通过 `requestLocationUpdates` 实现位置的持续更新
- **权限适配**: 使用 `ActivityResultContracts.RequestMultiplePermissions` 处理运行时权限请求

---

### 功能三：搜索路径导航（待实现）

#### 2.3.1 功能描述

当用户在地图上选择一个目的地时，应用能够显示从当前位置到目标地点的导航路径。

#### 2.3.2 当前状态

**此功能尚未实现**，目前点击导航按钮会显示提示：

```kotlin
root.findViewById<View>(R.id.btnNavigate)?.setOnClickListener {
    Toast.makeText(context, "导航功能将在后续版本开放", Toast.LENGTH_SHORT).show()
}
```

#### 2.3.3 预期实现方案

**技术选型**: 使用高德地图步行路径规划 SDK

**实现思路**:

1. **路径规划 API**: 调用高德地图 `WalkingRouteSearch` 进行步行路径规划
2. **路径绘制**: 使用 `AMap.addPolyline()` 绘制导航路径线
3. **模拟导航**: 沿路径逐步移动定位点，实现导航效果

**预期核心代码结构**:

```kotlin
// 路径搜索器
private fun searchRoute(
    startLatLng: LatLng,
    endLatLng: LatLng
) {
    val from = LatLonPoint(startLatLng.latitude, startLatLng.longitude)
    val to = LatLonPoint(endLatLng.latitude, endLatLng.longitude)
    val query = WalkingRouteQuery(from, to, WalkingRouteSearch.WALKING_DEFAULT)
    routeSearch.calculateRoute(query)
}

// 路径结果回调
private inner class RouteSearchListener : WalkingRouteSearchListener {
    override fun onWalkingRouteSearched(result: WalkingRouteResult?, errorCode: Int) {
        if (errorCode == AMapException.CODE_OK && result != null) {
            // 清除旧路径
            routeOverlay?.remove()
            // 绘制新路径
            val path = result.routes[0].walkingParcel
            routeOverlay = AMap.addPolyline(PolylineOptions()
                .add(*path.toTypedArray())
                .color(Color.BLUE)
                .width(10f))
        }
    }
}
```

#### 2.3.4 后续开发要点

| 模块 | 说明 |
|------|------|
| **高德路径规划 SDK** | 引入 `com.amap.api:search` 依赖 |
| **导航 UI** | 底部导航面板，显示预计时间、距离、转弯指引 |
| **实时定位** | 沿路径实时更新当前位置标记 |
| **路径绑路** | 将用户位置绑定到最近的导航路径点 |

---

## 三、项目架构

### 3.1 模块结构

```
com.example.guet_map/
├── di/                    # Hilt 依赖注入模块
├── local/                 # Room 数据库（地点、指引数据）
├── model/                 # 数据模型（Location, GuideStep）
├── network/               # Retrofit API 服务与 Mock 数据
├── repository/            # 数据仓库（缓存策略）
└── ui/
    ├── map/               # 地图模块（Fragment, ViewModel, Adapter）
    ├── contribute/        # UGC 贡献模块
    └── MainActivity.kt   # 应用入口
```

### 3.2 数据流向

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   UI 层     │ ←→ │  ViewModel   │ ←→ │ Repository  │
│ (Fragment)  │    │   (Flow)     │    │ (数据聚合)   │
└─────────────┘    └─────────────┘    └──────┬──────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    ▼                        ▼                        ▼
             ┌─────────────┐          ┌─────────────┐          ┌─────────────┐
             │   Room DB   │          │  API Service │          │  AMap SDK   │
             │  (本地缓存)   │          │  (网络请求)   │          │  (地图/定位) │
             └─────────────┘          └─────────────┘          └─────────────┘
```

---

## 四、已完成功能清单

| 序号 | 功能模块 | 状态 | 说明 |
|------|---------|------|------|
| 1 | 地点搜索 | ✅ 已完成 | 支持模糊搜索，回车居中显示 |
| 2 | 当前位置定位 | ✅ 已完成 | 系统 LocationManager + GPS/网络定位 |
| 3 | 路径导航 | ⏳ 待开发 | 需集成高德路径规划 SDK |
| 4 | 地点详情展示 | ✅ 已完成 | BottomSheet 展示地点信息 |
| 5 | 图文指引 | ✅ 已完成 | Room 缓存 + API 获取 |

---

## 五、后续开发计划

### 第二阶段

1. **导航路径功能**
   - 集成高德地图路径规划 SDK
   - 实现步行导航路径绘制
   - 添加导航引导面板

2. **导航实时跟踪**
   - GPS 位置沿路径实时更新
   - 转弯提醒与距离提示

---

## 六、关键文件索引

| 功能 | 文件路径 |
|------|---------|
| 地图核心 | `app/src/main/java/com/example/guet_map/ui/map/MapFragment.kt` |
| 地图逻辑 | `app/src/main/java/com/example/guet_map/ui/map/MapViewModel.kt` |
| 搜索适配器 | `app/src/main/java/com/example/guet_map/ui/map/SearchResultAdapter.kt` |
| 地点数据模型 | `app/src/main/java/com/example/guet_map/model/Location.kt` |
| 数据仓库 | `app/src/main/java/com/example/guet_map/repository/LocationRepository.kt` |
| 数据库配置 | `app/src/main/java/com/example/guet_map/local/AppDatabase.kt` |
| 依赖注入 | `app/src/main/java/com/example/guet_map/di/AppModule.kt` |
| 网络服务 | `app/src/main/java/com/example/guet_map/network/ApiService.kt` |
| Mock 数据 | `app/src/main/java/com/example/guet_map/network/MockInterceptor.kt` |

---

*文档由 AI 自动生成，如有问题请联系项目维护者*
