package com.example.guet_map.ui.contribute

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.local.dao.GuideStepDao
import com.example.guet_map.local.entity.GuideStepEntity
import com.example.guet_map.model.Location
import com.example.guet_map.model.MyGuideSubmission
import com.example.guet_map.model.Resource
import com.example.guet_map.model.UploadResponse
import com.example.guet_map.network.ApiService
import com.example.guet_map.repository.ContributeDraftRepository
import com.example.guet_map.repository.DraftStepData
import com.example.guet_map.repository.LocationRepository
import com.example.guet_map.util.ImageCompressor
import com.example.guet_map.util.LocalNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class ContributeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val locationRepository: LocationRepository,
    private val userPrefs: UserPrefs,
    private val draftRepository: ContributeDraftRepository,
    private val guideStepDao: GuideStepDao
) : ViewModel() {

    init {
        loadLocationsIfEmpty()
    }

    val cachedLocations: StateFlow<List<Location>> = locationRepository
        .observeCachedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLocationId = MutableStateFlow("")
    val selectedLocationId: StateFlow<String> = _selectedLocationId.asStateFlow()

    private val _selectedLocationName = MutableStateFlow("")
    val selectedLocationName: StateFlow<String> = _selectedLocationName.asStateFlow()

    private val _stepItems = MutableStateFlow(listOf(StepFormItem(stepNumber = 1)))
    val stepItems: StateFlow<List<StepFormItem>> = _stepItems.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    private val _userPoints = MutableStateFlow(userPrefs.points)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _pendingPhotoStepId = MutableStateFlow<String?>(null)
    val pendingPhotoStepId: StateFlow<String?> = _pendingPhotoStepId.asStateFlow()

    private val _mySubmissions = MutableStateFlow<Resource<List<MyGuideSubmission>>>(Resource.Loading)
    val mySubmissions: StateFlow<Resource<List<MyGuideSubmission>>> = _mySubmissions.asStateFlow()

    private fun loadLocationsIfEmpty() {
        viewModelScope.launch {
            if (cachedLocations.value.isEmpty()) {
                locationRepository.getLocations().collect { /* cache */ }
            }
        }
    }

    fun selectLocation(locationId: String, locationName: String) {
        _selectedLocationId.value = locationId
        _selectedLocationName.value = locationName
    }

    fun addStep() {
        val current = _stepItems.value
        val nextNum = current.size + 1
        _stepItems.value = current + StepFormItem(stepNumber = nextNum)
    }

    fun removeStep(stepId: String) {
        val filtered = _stepItems.value.filter { it.id != stepId }
        if (filtered.isEmpty()) {
            _stepItems.value = listOf(StepFormItem(stepNumber = 1))
        } else {
            _stepItems.value = filtered.mapIndexed { index, item ->
                item.copy(stepNumber = index + 1)
            }
        }
    }

    fun updateDescription(stepId: String, description: String) {
        _stepItems.value = _stepItems.value.map {
            if (it.id == stepId) it.copy(description = description) else it
        }
    }

    fun setImageUri(stepId: String, uri: Uri) {
        _stepItems.value = _stepItems.value.map {
            if (it.id == stepId) it.copy(imageUri = uri) else it
        }
    }

    fun setPendingPhotoStep(stepId: String?) {
        _pendingPhotoStepId.value = stepId?.takeIf { it.isNotBlank() }
    }

    fun saveDraft() {
        viewModelScope.launch {
            val steps = _stepItems.value.map { step ->
                DraftStepData(
                    stepNumber = step.stepNumber,
                    description = step.description,
                    imageUri = step.imageUri?.toString()
                )
            }
            draftRepository.saveDraft(
                com.example.guet_map.repository.ContributeDraft(
                    locationId = _selectedLocationId.value,
                    locationName = _selectedLocationName.value,
                    steps = steps
                )
            )
            _uploadState.value = UploadUiState.Success("草稿已保存")
        }
    }

    fun loadDraft() {
        viewModelScope.launch {
            val draft = draftRepository.loadDraft() ?: run {
                _uploadState.value = UploadUiState.Error("没有可恢复的草稿")
                return@launch
            }
            _selectedLocationId.value = draft.locationId
            _selectedLocationName.value = draft.locationName
            _stepItems.value = draft.steps.map { data ->
                StepFormItem(
                    stepNumber = data.stepNumber,
                    description = data.description,
                    imageUri = data.imageUri?.let { Uri.parse(it) }
                )
            }.ifEmpty { listOf(StepFormItem(stepNumber = 1)) }
            _uploadState.value = UploadUiState.Success("草稿已恢复")
        }
    }

    fun loadMySubmissions() {
        viewModelScope.launch {
            _mySubmissions.value = Resource.Loading
            try {
                _mySubmissions.value = Resource.Success(apiService.getMyGuideSubmissions())
            } catch (e: Exception) {
                _mySubmissions.value = Resource.Error("加载失败: ${e.localizedMessage}")
            }
        }
    }

    fun submitSteps() {
        val locationId = _selectedLocationId.value
        val locationName = _selectedLocationName.value
        val steps = _stepItems.value

        if (locationId.isBlank() && locationName.isBlank()) {
            _uploadState.value = UploadUiState.Error("请选择或输入目标地点")
            return
        }

        val filledSteps = steps.filter { it.description.isNotBlank() }
        if (filledSteps.isEmpty()) {
            _uploadState.value = UploadUiState.Error("请至少填写一个步骤的描述")
            return
        }

        val targetId = locationId.ifBlank { locationName }

        viewModelScope.launch {
            _uploadState.value = UploadUiState.Loading
            try {
                val entities = filledSteps.map { step ->
                    GuideStepEntity(
                        locationId = targetId,
                        stepNumber = step.stepNumber,
                        description = step.description,
                        imageUrl = step.imageUri?.toString().orEmpty()
                    )
                }
                guideStepDao.deleteByLocation(targetId)
                guideStepDao.insertAll(entities)

                userPrefs.addPoints(filledSteps.size * 2)
                userPrefs.contributionCount = userPrefs.contributionCount + filledSteps.size
                _userPoints.value = userPrefs.points
                draftRepository.clearDraft()
                val msg = "提交成功！已立即生效"
                _uploadState.value = UploadUiState.Success(msg)
                LocalNotificationHelper.show(
                    context,
                    "指路已生效",
                    "您提交的路线步骤已保存，可在导航页直接查看"
                )

                _selectedLocationId.value = ""
                _selectedLocationName.value = ""
                _stepItems.value = listOf(StepFormItem(stepNumber = 1))
            } catch (e: Exception) {
                _uploadState.value = UploadUiState.Error(
                    "提交失败: ${e.localizedMessage ?: "本地保存失败"}"
                )
            }
        }
    }

    fun clearUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    private fun createImagePart(step: StepFormItem): MultipartBody.Part {
        val uri = step.imageUri
        if (uri != null) {
            val bytes = ImageCompressor.compressToJpegBytes(context, uri)
            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            return MultipartBody.Part.createFormData(
                "image", "step_${step.stepNumber}.jpg", body
            )
        } else {
            val emptyBody = ByteArray(0).toRequestBody("image/jpeg".toMediaType())
            return MultipartBody.Part.createFormData("image", "empty.jpg", emptyBody)
        }
    }
}

sealed class UploadUiState {
    data object Idle : UploadUiState()
    data object Loading : UploadUiState()
    data class Success(val message: String) : UploadUiState()
    data class Error(val message: String) : UploadUiState()
}
