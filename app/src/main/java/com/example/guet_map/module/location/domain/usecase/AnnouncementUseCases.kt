package com.example.guet_map.module.location.domain.usecase

import com.example.guet_map.module.location.data.model.Announcement
import com.example.guet_map.module.location.data.model.AnnouncementCategory
import com.example.guet_map.module.location.data.repository.AnnouncementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取公告列表用例
 */
class GetAnnouncementsUseCase @Inject constructor(
    private val announcementRepository: AnnouncementRepository
) {
    operator fun invoke(): Flow<List<Announcement>> {
        return announcementRepository.getAllAnnouncements()
    }

    fun byCategory(category: AnnouncementCategory): Flow<List<Announcement>> {
        return announcementRepository.getAnnouncementsByCategory(category)
    }
}

/**
 * 获取公告详情用例
 */
class GetAnnouncementDetailUseCase @Inject constructor(
    private val announcementRepository: AnnouncementRepository
) {
    suspend operator fun invoke(id: String): Announcement? {
        return announcementRepository.getAnnouncementById(id)
    }
}

/**
 * 刷新公告用例
 */
class RefreshAnnouncementsUseCase @Inject constructor(
    private val announcementRepository: AnnouncementRepository
) {
    suspend operator fun invoke() = announcementRepository.refreshAnnouncements()
}
