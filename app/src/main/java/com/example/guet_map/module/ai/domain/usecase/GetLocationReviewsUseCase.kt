package com.example.guet_map.module.ai.domain.usecase

import com.example.guet_map.module.ai.data.model.Review
import com.example.guet_map.module.ai.data.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取地点评论用例
 */
class GetLocationReviewsUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    operator fun invoke(locationId: String): Flow<List<Review>> {
        return reviewRepository.getReviewsByLocation(locationId)
    }
}
