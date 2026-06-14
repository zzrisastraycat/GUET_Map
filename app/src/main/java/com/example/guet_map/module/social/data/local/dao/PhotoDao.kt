package com.example.guet_map.module.social.data.local.dao

import androidx.room.*
import com.example.guet_map.module.social.data.local.entity.PhotoAlbumEntity
import com.example.guet_map.module.social.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * 照片 DAO
 */
@Dao
interface PhotoDao {

    // 相册操作
    @Query("SELECT * FROM photo_albums ORDER BY updatedAt DESC")
    fun getAllAlbums(): Flow<List<PhotoAlbumEntity>>

    @Query("SELECT * FROM photo_albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: String): PhotoAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: PhotoAlbumEntity)

    @Update
    suspend fun updateAlbum(album: PhotoAlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: PhotoAlbumEntity)

    // 照片操作
    @Query("SELECT * FROM photos WHERE albumId = :albumId ORDER BY createdAt DESC")
    fun getPhotosByAlbum(albumId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE albumId = :albumId")
    suspend fun deletePhotosByAlbum(albumId: String)
}
