package com.example.guet_map.module.ai.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.ai.data.model.Review
import com.example.guet_map.module.ai.domain.usecase.GetLocationReviewsUseCase
import com.example.guet_map.module.ai.domain.usecase.PostReviewUseCase
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地点评论 ViewModel
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val getLocationReviewsUseCase: GetLocationReviewsUseCase,
    private val postReviewUseCase: PostReviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private var currentLocationId: String? = null

    fun loadReviews(locationId: String) {
        currentLocationId = locationId
        viewModelScope.launch {
            getLocationReviewsUseCase(locationId).collect { reviewList ->
                _reviews.value = reviewList
                _uiState.value = if (reviewList.isEmpty()) {
                    ReviewUiState.Empty
                } else {
                    ReviewUiState.Success(reviewList)
                }
            }
        }
    }

    fun postReview(
        userId: String,
        userName: String,
        rating: Float,
        content: String,
        images: List<String> = emptyList()
    ) {
        val locationId = currentLocationId ?: return

        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading

            when (val result = postReviewUseCase(locationId, userId, userName, rating, content, images)) {
                is Resource.Success -> {
                    _uiState.value = ReviewUiState.PostSuccess
                }
                is Resource.Error -> {
                    _uiState.value = ReviewUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetState() {
        _uiState.value = if (_reviews.value.isEmpty()) {
            ReviewUiState.Empty
        } else {
            ReviewUiState.Success(_reviews.value)
        }
    }
}

sealed class ReviewUiState {
    data object Loading : ReviewUiState()
    data object Empty : ReviewUiState()
    data class Success(val reviews: List<Review>) : ReviewUiState()
    data object PostSuccess : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}
