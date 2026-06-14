package com.example.guet_map.module.location.data.repository

import android.content.Context
import com.example.guet_map.module.location.data.local.dao.OfflineMapDao
import com.example.guet_map.module.location.data.local.entity.OfflineMapEntity
import com.example.guet_map.module.location.data.model.DownloadStatus
import com.example.guet_map.module.location.data.model.OfflineMap
import com.example.guet_map.model.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线地图仓库
 */
@Singleton
class OfflineMapRepository @Inject constructor(
    private val offlineMapDao: OfflineMapDao,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {

    fun getAllOfflineMaps(): Flow<List<OfflineMap>> {
        return offlineMapDao.getAllOfflineMaps().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getDownloadedMaps(): Flow<List<OfflineMap>> {
        return offlineMapDao.getDownloadedMaps().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getOfflineMapById(id: String): OfflineMap? {
        return offlineMapDao.getOfflineMapById(id)?.toDomain()
    }

    suspend fun downloadMap(
        mapId: String,
        onProgress: (Int, Long) -> Unit
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val map = offlineMapDao.getOfflineMapById(mapId) ?: return@withContext Resource.Error("地图不存在")

            offlineMapDao.updateDownloadProgress(mapId, DownloadStatus.DOWNLOADING.name, 0, 0)

            val request = Request.Builder()
                .url(map.downloadUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                offlineMapDao.updateDownloadProgress(mapId, DownloadStatus.FAILED.name, 0, 0)
                return@withContext Resource.Error("下载失败: ${response.code}")
            }

            val body = response.body ?: return@withContext Resource.Error("下载内容为空")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            val mapDir = File(context.filesDir, "offline_maps")
            if (!mapDir.exists()) mapDir.mkdirs()

            val outputFile = File(mapDir, "${mapId}.zip")

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0
                        onProgress(progress, downloadedBytes)
                        offlineMapDao.updateDownloadProgress(mapId, DownloadStatus.DOWNLOADING.name, progress, downloadedBytes)
                    }
                }
            }

            offlineMapDao.updateOfflineMap(
                OfflineMapEntity(
                    id = map.id,
                    name = map.name,
                    description = map.description,
                    version = map.version,
                    size = map.size,
                    downloadUrl = map.downloadUrl,
                    thumbnailUrl = map.thumbnailUrl,
                    localPath = outputFile.absolutePath,
                    status = DownloadStatus.DOWNLOADED.name,
                    progress = 100,
                    downloadedSize = totalBytes,
                    lastUpdateTime = System.currentTimeMillis()
                )
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            offlineMapDao.updateDownloadProgress(mapId, DownloadStatus.FAILED.name, 0, 0)
            Resource.Error(e.message ?: "下载失败")
        }
    }

    suspend fun deleteMap(mapId: String): Resource<Unit> {
        return try {
            val map = offlineMapDao.getOfflineMapById(mapId)
            map?.localPath?.let { path ->
                File(path).delete()
            }
            offlineMapDao.deleteOfflineMap(mapId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "删除失败")
        }
    }

    private fun OfflineMapEntity.toDomain() = OfflineMap(
        id = id,
        name = name,
        description = description,
        version = version,
        size = size,
        downloadUrl = downloadUrl,
        thumbnailUrl = thumbnailUrl,
        status = DownloadStatus.valueOf(status),
        progress = progress,
        downloadedSize = downloadedSize,
        lastUpdateTime = lastUpdateTime
    )
}
