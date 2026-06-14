package com.example.guet_map.module.ai.data.local.dao

import androidx.room.*
import com.example.guet_map.module.ai.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地点评论 DAO
 */
@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE locationId = :locationId ORDER BY createdAt DESC")
    fun getReviewsByLocation(locationId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReviewsByUser(userId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getReviewById(id: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Delete
    suspend fun deleteReview(review: ReviewEntity)

    @Query("UPDATE reviews SET likes = likes + 1 WHERE id = :reviewId")
    suspend fun incrementLikes(reviewId: String)
}
