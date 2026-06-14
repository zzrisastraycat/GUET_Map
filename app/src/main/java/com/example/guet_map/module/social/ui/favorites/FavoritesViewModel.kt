package com.example.guet_map.module.social.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.social.data.model.Favorite
import com.example.guet_map.module.social.data.model.FavoriteCategory
import com.example.guet_map.module.social.domain.usecase.GetFavoritesUseCase
import com.example.guet_map.module.social.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏列表 ViewModel
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FavoriteCategory.ALL)
    val selectedCategory: StateFlow<FavoriteCategory> = _selectedCategory.asStateFlow()

    // TODO: 从登录模块获取
    private val userId = "current_user_id"

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _selectedCategory.collectLatest { category ->
                getFavoritesUseCase.byCategory(userId, category).collect { list ->
                    _favorites.value = list
                    _uiState.value = if (list.isEmpty()) {
                        FavoritesUiState.Empty
                    } else {
                        FavoritesUiState.Success(list)
                    }
                }
            }
        }
    }

    fun selectCategory(category: FavoriteCategory) {
        _selectedCategory.value = category
    }

    fun removeFavorite(favorite: Favorite) {
        viewModelScope.launch {
            removeFavoriteUseCase(userId, favorite.locationId)
        }
    }
}

sealed class FavoritesUiState {
    data object Loading : FavoritesUiState()
    data object Empty : FavoritesUiState()
    data class Success(val favorites: List<Favorite>) : FavoritesUiState()
}
