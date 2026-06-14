package com.example.guet_map.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.model.Location
import com.example.guet_map.repository.LegacyFavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: LegacyFavoriteRepository
) : ViewModel() {

    val favorites: StateFlow<List<Location>> = favoriteRepository
        .observeFavoriteLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            favoriteRepository.syncFromServer()
        }
    }

    fun removeFavorite(locationId: String) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(locationId)
        }
    }
}
