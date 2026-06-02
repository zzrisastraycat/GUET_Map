# GUET Map — 桂林电子科技大学校园微导航

## 项目概述

GUET Map 是一款面向桂林电子科技大学（桂电）花江校区的校园微导航 Android 原生应用。核心功能是通过"实景图文指引"（类似商家指路）解决传统地图在校园内最后 50 米寻路困难的痛点。

**产品模式**：官方提供基础地图 POI + 用户共创（UGC）实景导航步骤，审核通过后获得积分奖励。

**技术栈**：Kotlin + MVVM + Hilt + Room + Retrofit + Coil + 高德 3D 地图 SDK

**Base URL**（开发阶段 Mock，对接后替换）：
```
https://api.guetmap.example.com
```

---

## 目录结构

```
app/src/main/java/com/example/guet_map/
├── GUETMapApplication.kt        # Hilt Application，高德隐私合规初始化
├── MainActivity.kt              # 单 Activity，BottomNavigationView + NavHost
├── di/
│   └── AppModule.kt             # Hilt 模块：OkHttp, Retrofit, Room, DAO
├── model/
│   ├── Location.kt              # 地点领域模型
│   ├── GuideStep.kt             # 指引步骤领域模型
│   ├── Resource.kt              # 网络状态封装 (Loading / Success / Error)
│   └── UploadResponse.kt        # UGC 上传响应
├── network/
│   ├── ApiService.kt            # Retrofit API 接口定义（后端对接着重看此文件）
│   └── MockInterceptor.kt       # 开发阶段 Mock，对接后删除
├── local/
│   ├── entity/
│   │   ├── LocationEntity.kt    # Room Entity
│   │   └── GuideStepEntity.kt
│   ├── dao/
│   │   ├── LocationDao.kt
│   │   └── GuideStepDao.kt
│   └── AppDatabase.kt           # Room Database (v2)
├── repository/
│   ├── LocationRepository.kt    # SSOT：网络优先 → Room 缓存
│   └── GuideRepository.kt
├── data/
│   └── UserPrefs.kt             # SharedPreferences：登录态、积分
└── ui/
    ├── map/
    │   ├── MapFragment.kt       # 地图主页：高德 MapView + BottomSheet + 搜索
    │   ├── MapViewModel.kt      # 地图状态管理：地点、筛选、Marker、搜索
    │   ├── GuideStepAdapter.kt  # 图文步骤 RecyclerView 适配器
    │   ├── FilterTagAdapter.kt  # 水平筛选标签
    │   └── SearchResultAdapter.kt # 搜索结果适配器
    └── contribute/
        ├── ContributeFragment.kt    # UGC 提交页面
        ├── ContributeViewModel.kt   # 表单状态 + Multipart 上传
        └── ContributeStepAdapter.kt # 步骤表单适配器
```

---

## API 接口文档

### 1. 获取所有地点

```
GET /api/v1/locations
```

**响应示例**：
```json
[
  {
    "locationId": "building_11b",
    "name": "第十一教学楼B区",
    "latitude": 25.2857,
    "longitude": 110.4141,
    "category": "教室",
    "rating": 4.5,
    "openingHours": "07:00-22:30",
    "imageUrl": "https://cdn.guetmap.example.com/img/11b.jpg",
    "hasGuide": true
  }
]
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `locationId` | String | 唯一标识，如 `building_11b`、`canteen_1` |
| `name` | String | 地点名称 |
| `latitude` | Double | 纬度（GCJ-02 坐标系，匹配高德地图） |
| `longitude` | Double | 经度 |
| `category` | String | 分类，当前值：教室 / 食堂 / 咖啡 / 图书馆 / 宿舍 / 校门 / 商店 / 运动场 |
| `rating` | Float | 综合评分（0-5） |
| `openingHours` | String | 营业/开放时间，如 `"07:00-22:30"` |
| `imageUrl` | String | 封面图 URL，可为空字符串 |
| `hasGuide` | Boolean | 是否有图文指引，true 时前端展示"商家指路"入口 |

---

### 2. 按分类筛选地点

```
GET /api/v1/locations?category={category}
```

`category` 为中文分类名（如 `食堂`、`教室`），返回格式同接口 1。

---

### 3. 获取地点详情

```
GET /api/v1/locations/{locationId}
```

返回单个 Location 对象，格式同接口 1。不存在时返回 404。

---

### 4. 获取图文指引步骤

```
GET /api/v1/locations/{locationId}/guides
```

**响应示例**：
```json
[
  {
    "id": 1,
    "locationId": "building_11b",
    "stepNumber": 1,
    "description": "从南门进入校园，沿主干道直行约200米",
    "imageUrl": "https://cdn.guetmap.example.com/guide/11b_01.jpg"
  },
  {
    "id": 2,
    "locationId": "building_11b",
    "stepNumber": 2,
    "description": "看到「创新大楼」指示牌后左转",
    "imageUrl": "https://cdn.guetmap.example.com/guide/11b_02.jpg"
  }
]
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 步骤主键 |
| `locationId` | String | 所属地点 ID |
| `stepNumber` | Int | 步骤序号（从 1 开始递增） |
| `description` | String | 步骤文字描述 |
| `imageUrl` | String | 实景照片，可为空字符串（纯文字步骤） |

**分页版本**：
```
GET /api/v1/locations/{locationId}/guides?page=1&size=20
```

---

### 5. UGC 上传指引步骤

```
POST /api/v1/guides/upload
Content-Type: multipart/form-data
```

**表单字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `locationId` | String | 目标地点 ID（若用户输入的为新地点则传地点名称） |
| `stepNumber` | String | 步骤序号 |
| `description` | String | 步骤文字描述 |
| `image` | File | 实景照片（可选，`image/jpeg` 或 `image/png`） |

**响应示例**：
```json
{
  "success": true,
  "message": "上传成功，待审核通过后将发放积分",
  "pointsAwarded": 5
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | Boolean | 是否提交成功 |
| `message` | String | 提示信息 |
| `pointsAwarded` | Int | 本次获得的积分（审核通过后实际发放） |

---

## 数据架构

### SSOT（Single Source of Truth）策略

所有数据流遵循 **网络优先 → Room 缓存 → UI 观察** 模式：

```
ApiService (Retrofit)
    ↓ 请求成功
Room Database (缓存)
    ↓ Flow 实时推送
ViewModel (StateFlow)
    ↓ collectLatest
Fragment (UI)
```

- **网络成功**：写入 Room → 通知 UI 刷新
- **网络失败**：自动回退 Room 缓存，用户仍可浏览历史数据
- **离线可用**：已浏览过的地点和指引全部缓存在本地

### 本地数据库

**locations 表**（Room：`tableName = "locations"`）：

| 列名 | 类型 | 约束 |
|------|------|------|
| `location_id` | TEXT | PRIMARY KEY |
| `name` | TEXT | NOT NULL |
| `latitude` | REAL | |
| `longitude` | REAL | |
| `category` | TEXT | |
| `rating` | REAL | DEFAULT 0 |
| `opening_hours` | TEXT | DEFAULT '' |
| `image_url` | TEXT | DEFAULT '' |
| `has_guide` | INTEGER | DEFAULT 0 (boolean) |

**guide_steps 表**（Room：`tableName = "guide_steps"`）：

| 列名 | 类型 | 约束 |
|------|------|------|
| `id` | INTEGER | PRIMARY KEY AUTO_INCREMENT |
| `location_id` | TEXT | NOT NULL, INDEXED |
| `step_number` | INTEGER | NOT NULL |
| `description` | TEXT | NOT NULL |
| `image_url` | TEXT | DEFAULT '' |

---

## 对接清单

接入真实后端时需要做的修改：

### 必须修改

1. **`di/AppModule.kt:23`** — 将 `BASE_URL` 改为实际 API 地址
2. **`di/AppModule.kt:30`** — 删除 `MockInterceptor()`，移除 `.addInterceptor(MockInterceptor())` 这一行
3. **`local/AppDatabase.kt`** — 如正式版不需要 `fallbackToDestructiveMigration()`，改为 `Migration` 策略

### 可选修改

4. **认证** — 当前无 Token 机制，如需用户认证：
   - 在 `OkHttpClient.Builder()` 中添加 `Interceptor` 注入 JWT/Bearer Token
   - 或在 `UserPrefs.kt` 中存储 Token
5. **图片上传** — 当前直接上传原始文件，生产环境建议：
   - 客户端先压缩（可引入 `libs.compress`）
   - 或后端返回 OSS 预签名 URL，客户端直传对象存储
6. **分类枚举** — 当前 category 为自由文本，建议后端维护一个分类字典接口

### 删除文件

对接完成后可安全删除：
- `network/MockInterceptor.kt`（整个文件）
- `app/build.gradle.kts` 中 `ndk.abiFilters` 块（如不用 x86 模拟器）

---

## 技术栈版本

| 组件 | 版本 |
|------|------|
| AGP | 8.9.0 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Hilt | 2.51.1 |
| Room | 2.6.1 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Coil | 2.7.0 |
| 高德 3D SDK | 10.0.600 |
| minSdk / targetSdk | 24 / 35 |

---

## 坐标系说明

使用 **GCJ-02** 坐标系（高德地图原生坐标系）。后端存储的经纬度必须是 GCJ-02，不要使用 WGS-84（GPS 原始坐标），否则在 3D 地图上会有约 300 米的偏移。

---

## P0 功能：当前位置显示

### 技术方案

使用 Android 系统 `LocationManager` 获取 GPS/Network 定位（放弃高德内置定位引擎，因其在远离校园时频繁返回无效坐标导致白屏）。

### 实现要点

| 要点 | 说明 |
|------|------|
| 定位源 | `LocationManager.requestLocationUpdates()`，从所有可用 provider 拉取，2000ms/5m |
| 定位图标 | 自定义蓝色靶心图标 `ic_my_location.xml`，通过 `BitmapDescriptorFactory.fromResource()` 渲染 |
| 地图蓝点 | 已关闭高德内置 `myLocationStyle`（`setMyLocationEnabled(false)`），只使用自定义 Marker |
| 归位按钮 | FAB（`fabMyLocation`，右下角 48dp），点击调用 `moveCamera`（非 `animateCamera`）缩放到 zoom 17 |
| 位置监听器 | 存储为 Fragment 字段，`onDestroyView()` 中调用 `removeUpdates()` 正确移除 |

### 权限处理

- 首次启动：显示权限说明弹窗 → 调用 `ActivityResultContracts.RequestMultiplePermissions()` → 同时请求 `ACCESS_FINE_LOCATION` 和 `ACCESS_COARSE_LOCATION`
- 已授权返回用户：`Application.onCreate()` 中检查 SharedPreferences 缓存状态

### 相关文件

- `MapFragment.kt:274-316` — 定位逻辑
- `res/drawable/ic_my_location.xml` — 定位图标
- `res/layout/fragment_map.xml:84-97` — 归位 FAB

---

## P0 功能：关键词搜索

### 技术方案

使用 Kotlin Flow `combine` 运算符将搜索查询与本地地点缓存实时组合，实现输入即搜的模糊匹配。

### 数据流

```
EditText (TextWatcher)
    ↓ setSearchQuery()
_searchQuery (MutableStateFlow<String>)
    ↓ combine
searchResults (StateFlow<List<Location>>)   ← cachedLocations (Room Flow)
    ↓ collectLatest
SearchResultAdapter → RecyclerView 下拉卡片
    ↓ 点击结果
移动相机 + 展开 BottomSheet + 清空搜索
```

### 实现要点

| 要点 | 说明 |
|------|------|
| 匹配方式 | `String.contains(query, ignoreCase = true)` 纯内存模糊匹配 |
| 数据源 | `cachedLocations`（Room Flow），无需网络请求 |
| 输入防抖 | 无（轻量纯内存匹配，无需节流） |
| 结果 UI | CardView 内嵌 RecyclerView，位于搜索栏正下方，浮于筛选标签之上（elevation 6dp） |
| 空查询 | 返回空列表，下拉卡片自动隐藏（gone） |
| 选中处理 | 清空输入框 → 收起键盘 → 移动相机到目标位置（zoom 17） → 调用 `selectLocation()` 展开详情 |

### 显示/隐藏策略

- `searchResults.isNotEmpty()` → `cardSearchResults.visibility = VISIBLE` + `submitList()`
- `searchResults.isEmpty()` → `cardSearchResults.visibility = GONE`
- 用户点击结果后主动调用 `hideSearchResults()`：清空 EditText + 隐藏键盘

### 相关文件

- `MapViewModel.kt:82-97` — searchQuery / searchResults StateFlow
- `MapFragment.kt:496-533` — TextWatcher + 搜索 UI 逻辑
- `SearchResultAdapter.kt` — 搜索结果适配器
- `res/layout/item_search_result.xml` — 搜索结果行布局（名称 + 分类标签）
- `res/layout/fragment_map.xml:72-87` — 搜索结果下拉卡片
