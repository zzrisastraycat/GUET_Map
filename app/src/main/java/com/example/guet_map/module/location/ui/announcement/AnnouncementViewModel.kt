package com.example.guet_map.module.location.ui.announcement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.location.data.model.Announcement
import com.example.guet_map.module.location.data.model.AnnouncementCategory
import com.example.guet_map.module.location.domain.usecase.GetAnnouncementsUseCase
import com.example.guet_map.module.location.domain.usecase.RefreshAnnouncementsUseCase
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 公告列表 ViewModel
 */
@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val getAnnouncementsUseCase: GetAnnouncementsUseCase,
    private val refreshAnnouncementsUseCase: RefreshAnnouncementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnnouncementUiState>(AnnouncementUiState.Loading)
    val uiState: StateFlow<AnnouncementUiState> = _uiState.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AnnouncementCategory?>(null)
    val selectedCategory: StateFlow<AnnouncementCategory?> = _selectedCategory.asStateFlow()

    init {
        loadAnnouncements()
    }

    private fun loadAnnouncements() {
        viewModelScope.launch {
            _selectedCategory.collectLatest { category ->
                val flow = if (category != null) {
                    getAnnouncementsUseCase.byCategory(category)
                } else {
                    getAnnouncementsUseCase()
                }

                flow.collect { list ->
                    _announcements.value = list
                    _uiState.value = if (list.isEmpty()) {
                        AnnouncementUiState.Empty
                    } else {
                        AnnouncementUiState.Success(list)
                    }
                }
            }
        }
    }

    fun selectCategory(category: AnnouncementCategory?) {
        _selectedCategory.value = category
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = AnnouncementUiState.Refreshing

            when (val result = refreshAnnouncementsUseCase()) {
                is Resource.Success -> {
                    // 数据通过 Flow 更新
                    _uiState.value = if (_announcements.value.isEmpty()) {
                        AnnouncementUiState.Empty
                    } else {
                        AnnouncementUiState.Success(_announcements.value)
                    }
                }
                is Resource.Error -> {
                    _uiState.value = AnnouncementUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }
}

sealed class AnnouncementUiState {
    data object Loading : AnnouncementUiState()
    data object Refreshing : AnnouncementUiState()
    data object Empty : AnnouncementUiState()
    data class Success(val announcements: List<Announcement>) : AnnouncementUiState()
    data class Error(val message: String) : AnnouncementUiState()
}
