package com.example.guet_map.core.user.model

/**
 * 用户信息
 */
data class User(
    val id: String,
    val name: String,
    val studentId: String? = null,  // 学号
    val avatar: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val department: String? = null,  // 院系
    val major: String? = null,       // 专业
    val grade: String? = null,       // 年级
    val role: UserRole = UserRole.STUDENT,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 用户角色
 */
enum class UserRole {
    STUDENT,   // 学生
    TEACHER,   // 教师
    ADMIN,     // 管理员
    VISITOR    // 访客
}
