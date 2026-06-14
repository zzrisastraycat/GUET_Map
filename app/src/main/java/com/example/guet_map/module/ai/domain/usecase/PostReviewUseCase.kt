package com.example.guet_map.module.ai.domain.usecase

import com.example.guet_map.module.ai.data.model.Review
import com.example.guet_map.module.ai.data.repository.ReviewRepository
import com.example.guet_map.model.Resource
import javax.inject.Inject

/**
 * 发布评论用例
 */
class PostReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(
        locationId: String,
        userId: String,
        userName: String,
        rating: Float,
        content: String,
        images: List<String> = emptyList()
    ): Resource<Review> {
        return try {
            if (content.isBlank()) {
                return Resource.Error("评论内容不能为空")
            }
            if (rating < 1f || rating > 5f) {
                return Resource.Error("评分必须在 1-5 之间")
            }

            val review = reviewRepository.createReview(
                locationId = locationId,
                userId = userId,
                userName = userName,
                rating = rating,
                content = content.trim(),
                images = images
            )
            Resource.Success(review)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "发布评论失败")
        }
    }
}
