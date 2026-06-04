package com.example.guet_map.repository

import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.LoginRequest
import com.example.guet_map.model.LoginResponse
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs,
    private val favoriteRepository: FavoriteRepository
) {

    fun login(username: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading)
        try {
            val normalizedUser = username.trim().ifBlank { UserPrefs.GUEST_USER_ID }
            val response = apiService.login(LoginRequest(normalizedUser, password))
            userPrefs.login(normalizedUser, response)
            favoriteRepository.switchUser(normalizedUser)
            favoriteRepository.syncFromServer()
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("登录失败: ${e.localizedMessage}"))
        }
    }

    fun logout() {
        val previousUser = userPrefs.userId
        userPrefs.clearAll()
        userPrefs.userId = UserPrefs.GUEST_USER_ID
        favoriteRepository.switchUser(UserPrefs.GUEST_USER_ID)
    }

    val isLoggedIn: Boolean get() = userPrefs.isLoggedIn
    val nickname: String get() = userPrefs.nickname
    val userId: String get() = userPrefs.userId
    val points: Int get() = userPrefs.points
}
