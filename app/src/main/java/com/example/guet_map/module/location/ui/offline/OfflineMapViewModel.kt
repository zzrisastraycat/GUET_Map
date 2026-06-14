package com.example.guet_map.module.location.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.location.data.model.DownloadStatus
import com.example.guet_map.module.location.data.model.OfflineMap
import com.example.guet_map.module.location.data.repository.OfflineMapRepository
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 离线地图 ViewModel
 */
@HiltViewModel
class OfflineMapViewModel @Inject constructor(
    private val offlineMapRepository: OfflineMapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OfflineMapUiState>(OfflineMapUiState.Loading)
    val uiState: StateFlow<OfflineMapUiState> = _uiState.asStateFlow()

    private val _offlineMaps = MutableStateFlow<List<OfflineMap>>(emptyList())
    val offlineMaps: StateFlow<List<OfflineMap>> = _offlineMaps.asStateFlow()

    private val _downloadingMap = MutableStateFlow<Pair<String, Int>?>(null)  // mapId to progress
    val downloadingMap: StateFlow<Pair<String, Int>?> = _downloadingMap.asStateFlow()

    init {
        loadOfflineMaps()
    }

    private fun loadOfflineMaps() {
        viewModelScope.launch {
            offlineMapRepository.getAllOfflineMaps().collect { maps ->
                _offlineMaps.value = maps
                _uiState.value = if (maps.isEmpty()) {
                    OfflineMapUiState.Empty
                } else {
                    OfflineMapUiState.Success(maps)
                }
            }
        }
    }

    fun downloadMap(mapId: String) {
        viewModelScope.launch {
            offlineMapRepository.downloadMap(mapId) { progress, downloadedSize ->
                _downloadingMap.value = mapId to progress
            }.let { result ->
                _downloadingMap.value = null
                when (result) {
                    is Resource.Success -> {
                        // 下载完成，数据通过 Flow 更新
                    }
                    is Resource.Error -> {
                        _uiState.value = OfflineMapUiState.Error(result.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteMap(mapId: String) {
        viewModelScope.launch {
            offlineMapRepository.deleteMap(mapId)
        }
    }

    fun pauseDownload(mapId: String) {
        // TODO: 实现暂停下载
    }
}

sealed class OfflineMapUiState {
    data object Loading : OfflineMapUiState()
    data object Empty : OfflineMapUiState()
    data class Success(val maps: List<OfflineMap>) : OfflineMapUiState()
    data class Error(val message: String) : OfflineMapUiState()
}
