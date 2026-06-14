package com.example.guet_map.module.social.data.repository

import com.example.guet_map.module.social.data.local.dao.PhotoDao
import com.example.guet_map.module.social.data.local.entity.PhotoAlbumEntity
import com.example.guet_map.module.social.data.local.entity.PhotoEntity
import com.example.guet_map.module.social.data.model.Photo
import com.example.guet_map.module.social.data.model.PhotoAlbum
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 照片仓库
 */
@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao
) {

    // 相册操作
    fun getAllAlbums(): Flow<List<PhotoAlbum>> {
        return photoDao.getAllAlbums().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAlbumById(albumId: String): PhotoAlbum? {
        return photoDao.getAlbumById(albumId)?.toDomain()
    }

    suspend fun createAlbum(name: String, description: String? = null): Resource<PhotoAlbum> {
        return try {
            val album = PhotoAlbum(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            photoDao.insertAlbum(album.toEntity())
            Resource.Success(album)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "创建相册失败")
        }
    }

    suspend fun deleteAlbum(albumId: String): Resource<Unit> {
        return try {
            photoDao.getAlbumById(albumId)?.let { album ->
                photoDao.deletePhotosByAlbum(albumId)
                photoDao.deleteAlbum(album)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "删除相册失败")
        }
    }

    // 照片操作
    fun getPhotosByAlbum(albumId: String): Flow<List<Photo>> {
        return photoDao.getPhotosByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllPhotos(): Flow<List<Photo>> {
        return photoDao.getAllPhotos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addPhoto(
        albumId: String,
        url: String,
        thumbnailUrl: String? = null,
        width: Int,
        height: Int,
        size: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        capturedAt: Long? = null
    ): Resource<Photo> {
        return try {
            val photo = Photo(
                id = UUID.randomUUID().toString(),
                albumId = albumId,
                url = url,
                thumbnailUrl = thumbnailUrl,
                width = width,
                height = height,
                size = size,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                capturedAt = capturedAt
            )
            photoDao.insertPhoto(photo.toEntity())

            // 更新相册照片数
            photoDao.getAlbumById(albumId)?.let { album ->
                photoDao.updateAlbum(
                    album.copy(
                        photoCount = album.photoCount + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            Resource.Success(photo)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "添加照片失败")
        }
    }

    suspend fun deletePhoto(photo: Photo): Resource<Unit> {
        return try {
            photoDao.deletePhoto(photo.toEntity())

            // 更新相册照片数
            photoDao.getAlbumById(photo.albumId)?.let { album ->
                photoDao.updateAlbum(
                    album.copy(
                        photoCount = (album.photoCount - 1).coerceAtLeast(0),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "删除照片失败")
        }
    }

    private fun PhotoAlbumEntity.toDomain() = PhotoAlbum(
        id = id,
        name = name,
        coverUrl = coverUrl,
        photoCount = photoCount,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PhotoAlbum.toEntity() = PhotoAlbumEntity(
        id = id,
        name = name,
        coverUrl = coverUrl,
        photoCount = photoCount,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PhotoEntity.toDomain() = Photo(
        id = id,
        albumId = albumId,
        url = url,
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        size = size,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        capturedAt = capturedAt,
        createdAt = createdAt
    )

    private fun Photo.toEntity() = PhotoEntity(
        id = id,
        albumId = albumId,
        url = url,
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        size = size,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        capturedAt = capturedAt,
        createdAt = createdAt
    )
}
