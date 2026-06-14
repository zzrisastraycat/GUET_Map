package com.example.guet_map.core.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Toast 扩展
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * 日期格式化
 */
object DateFormatter {
    private val fullDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val shortDateTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatFullDateTime(timestamp: Long): String = fullDateTime.format(Date(timestamp))

    fun formatShortDateTime(timestamp: Long): String = shortDateTime.format(Date(timestamp))

    fun formatTimeOnly(timestamp: Long): String = timeOnly.format(Date(timestamp))

    fun formatDateOnly(timestamp: Long): String = dateOnly.format(Date(timestamp))

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} 分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} 小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} 天前"
            else -> formatShortDateTime(timestamp)
        }
    }
}

/**
 * 文件大小格式化
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

/**
 * 距离格式化
 */
fun formatDistance(meters: Float): String {
    return when {
        meters < 1000 -> "${meters.toInt()}m"
        else -> String.format("%.1fkm", meters / 1000)
    }
}
