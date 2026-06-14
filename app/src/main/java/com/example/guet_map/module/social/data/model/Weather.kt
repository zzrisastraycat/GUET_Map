package com.example.guet_map.module.social.data.model

/**
 * 天气数据
 */
data class Weather(
    val id: String,
    val temperature: Int,           // 温度（摄氏度）
    val feelsLike: Int,             // 体感温度
    val humidity: Int,               // 湿度 %
    val windSpeed: Float,            // 风速 m/s
    val windDirection: String,      // 风向
    val weatherType: WeatherType,
    val description: String,
    val aqi: Int? = null,           // 空气质量指数
    val aqiLevel: String? = null,   // 空气质量等级
    val uvIndex: Int? = null,       // 紫外线指数
    val sunrise: Long,              // 日出时间
    val sunset: Long,               // 日落时间
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val alertMessage: String? = null // 天气预警信息
)

/**
 * 天气类型
 */
enum class WeatherType(val icon: String, val description: String) {
    SUNNY("sunny", "晴"),
    CLOUDY("cloudy", "多云"),
    OVERCAST("overcast", "阴"),
    LIGHT_RAIN("light_rain", "小雨"),
    MODERATE_RAIN("moderate_rain", "中雨"),
    HEAVY_RAIN("heavy_rain", "大雨"),
    THUNDERSTORM("thunderstorm", "雷阵雨"),
    SNOW("snow", "雪"),
    FOG("fog", "雾"),
    WINDY("windy", "大风"),
    UNKNOWN("unknown", "未知")
}

/**
 * 小时天气预报
 */
data class HourlyWeather(
    val hour: Int,          // 小时 (0-23)
    val temperature: Int,
    val weatherType: WeatherType,
    val precipitation: Int   // 降水概率 %
)
