package com.example.guet_map.module.social.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.social.data.model.Photo
import com.example.guet_map.module.social.data.model.PhotoAlbum
import com.example.guet_map.module.social.data.repository.PhotoRepository
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 相册 ViewModel
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _albumsState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val albumsState: StateFlow<GalleryUiState> = _albumsState.asStateFlow()

    private val _albums = MutableStateFlow<List<PhotoAlbum>>(emptyList())
    val albums: StateFlow<List<PhotoAlbum>> = _albums.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<PhotoAlbum?>(null)
    val selectedAlbum: StateFlow<PhotoAlbum?> = _selectedAlbum.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            photoRepository.getAllAlbums().collect { albumList ->
                _albums.value = albumList
                _albumsState.value = if (albumList.isEmpty()) {
                    GalleryUiState.Empty
                } else {
                    GalleryUiState.Success(albumList)
                }
            }
        }
    }

    fun selectAlbum(album: PhotoAlbum) {
        _selectedAlbum.value = album
        viewModelScope.launch {
            photoRepository.getPhotosByAlbum(album.id).collect { photoList ->
                _photos.value = photoList
            }
        }
    }

    fun clearSelectedAlbum() {
        _selectedAlbum.value = null
        _photos.value = emptyList()
    }

    fun createAlbum(name: String, description: String? = null) {
        viewModelScope.launch {
            photoRepository.createAlbum(name, description)
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            photoRepository.deleteAlbum(albumId)
            if (_selectedAlbum.value?.id == albumId) {
                clearSelectedAlbum()
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
        }
    }
}

sealed class GalleryUiState {
    data object Loading : GalleryUiState()
    data object Empty : GalleryUiState()
    data class Success(val albums: List<PhotoAlbum>) : GalleryUiState()
}
