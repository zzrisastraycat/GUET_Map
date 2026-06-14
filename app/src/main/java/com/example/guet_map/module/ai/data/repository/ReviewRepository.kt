package com.example.guet_map.module.ai.data.repository

import com.example.guet_map.module.ai.data.local.dao.ReviewDao
import com.example.guet_map.module.ai.data.local.entity.ReviewEntity
import com.example.guet_map.module.ai.data.model.Review

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地点评论仓库
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val reviewDao: ReviewDao,
    private val gson: Gson
) {

    fun getReviewsByLocation(locationId: String): Flow<List<Review>> {
        return reviewDao.getReviewsByLocation(locationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getReviewsByUser(userId: String): Flow<List<Review>> {
        return reviewDao.getReviewsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getReviewById(id: String): Review? {
        return reviewDao.getReviewById(id)?.toDomain()
    }

    suspend fun saveReview(review: Review) {
        reviewDao.insertReview(review.toEntity())
    }

    suspend fun createReview(
        locationId: String,
        userId: String,
        userName: String,
        rating: Float,
        content: String,
        images: List<String> = emptyList()
    ): Review {
        val review = Review(
            id = UUID.randomUUID().toString(),
            locationId = locationId,
            userId = userId,
            userName = userName,
            rating = rating,
            content = content,
            images = images
        )
        reviewDao.insertReview(review.toEntity())
        return review
    }

    suspend fun likeReview(reviewId: String) {
        reviewDao.incrementLikes(reviewId)
    }

    suspend fun deleteReview(review: Review) {
        reviewDao.deleteReview(review.toEntity())
    }

    private fun ReviewEntity.toDomain() = Review(
        id = id,
        locationId = locationId,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar,
        rating = rating,
        content = content,
        images = gson.fromJson(images, object : TypeToken<List<String>>() {}.type) ?: emptyList(),
        likes = likes,
        createdAt = createdAt,
        replyCount = replyCount
    )

    private fun Review.toEntity() = ReviewEntity(
        id = id,
        locationId = locationId,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar,
        rating = rating,
        content = content,
        images = gson.toJson(images),
        likes = likes,
        createdAt = createdAt,
        replyCount = replyCount
    )
}
