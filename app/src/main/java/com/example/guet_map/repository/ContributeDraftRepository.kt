package com.example.guet_map.repository

import com.example.guet_map.core.dao.ContributeDraftDao
import com.example.guet_map.core.entity.ContributeDraftEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

data class DraftStepData(
    val stepNumber: Int,
    val description: String,
    val imageUri: String?
)

data class ContributeDraft(
    val locationId: String,
    val locationName: String,
    val steps: List<DraftStepData>
)

@Singleton
class ContributeDraftRepository @Inject constructor(
    private val draftDao: ContributeDraftDao,
    private val gson: Gson
) {

    suspend fun loadDraft(): ContributeDraft? {
        val entity = draftDao.getDraft() ?: return null
        if (entity.stepsJson.isBlank()) return null
        val type = object : TypeToken<List<DraftStepData>>() {}.type
        val steps: List<DraftStepData> = gson.fromJson(entity.stepsJson, type)
        return ContributeDraft(
            locationId = entity.locationId,
            locationName = entity.locationName,
            steps = steps
        )
    }

    suspend fun saveDraft(draft: ContributeDraft) {
        draftDao.save(
            ContributeDraftEntity(
                locationId = draft.locationId,
                locationName = draft.locationName,
                stepsJson = gson.toJson(draft.steps)
            )
        )
    }

    suspend fun clearDraft() {
        draftDao.clear()
    }
}
