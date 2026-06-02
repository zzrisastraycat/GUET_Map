package com.example.guet_map.repository

import com.example.guet_map.local.dao.GuideStepDao
import com.example.guet_map.local.entity.GuideStepEntity
import com.example.guet_map.model.GuideStep
import com.example.guet_map.model.Resource
import com.example.guet_map.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuideRepository @Inject constructor(
    private val apiService: ApiService,
    private val guideStepDao: GuideStepDao
) {

    /**
     * SSOT: 网络优先 → 成功后缓存到 Room → 网络失败时回退缓存。
     */
    fun getGuideSteps(locationId: String): Flow<Resource<List<GuideStep>>> = flow {
        emit(Resource.Loading)

        val hasCached = guideStepDao.countByLocation(locationId) > 0

        try {
            val remoteSteps = apiService.getGuideSteps(locationId)
            val entities = remoteSteps.map { it.toEntity() }
            guideStepDao.deleteByLocation(locationId)
            guideStepDao.insertAll(entities)
            emit(Resource.Success(remoteSteps))
        } catch (e: IOException) {
            if (hasCached) {
                val cached = guideStepDao.getGuideStepsByLocation(locationId).first()
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("网络不可用: ${e.localizedMessage}"))
            }
        } catch (e: Exception) {
            if (hasCached) {
                val cached = guideStepDao.getGuideStepsByLocation(locationId).first()
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("加载失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 观察 Room 中的缓存数据（实时更新）。
     */
    fun observeCachedGuideSteps(locationId: String): Flow<List<GuideStep>> {
        return guideStepDao.getGuideStepsByLocation(locationId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    private fun GuideStep.toEntity() = GuideStepEntity(
        locationId = locationId,
        stepNumber = stepNumber,
        description = description,
        imageUrl = imageUrl
    )

    private fun GuideStepEntity.toDomain() = GuideStep(
        id = id,
        locationId = locationId,
        stepNumber = stepNumber,
        description = description,
        imageUrl = imageUrl
    )
}
