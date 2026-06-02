package com.example.guet_map.ui.contribute

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.Location
import com.example.guet_map.model.Resource
import com.example.guet_map.model.UploadResponse
import com.example.guet_map.network.ApiService
import com.example.guet_map.repository.LocationRepository
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
    private val userPrefs: UserPrefs
) : ViewModel() {

    init {
        loadLocationsIfEmpty()
    }

    // ── 地点列表 (供 AutoComplete 下拉) ──────────────────────

    val cachedLocations: StateFlow<List<Location>> = locationRepository
        .observeCachedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 选中地点 ─────────────────────────────────────────────

    private val _selectedLocationId = MutableStateFlow("")
    val selectedLocationId: StateFlow<String> = _selectedLocationId.asStateFlow()

    private val _selectedLocationName = MutableStateFlow("")
    val selectedLocationName: StateFlow<String> = _selectedLocationName.asStateFlow()

    // ── 步骤表单 ─────────────────────────────────────────────

    private val _stepItems = MutableStateFlow(listOf(StepFormItem(stepNumber = 1)))
    val stepItems: StateFlow<List<StepFormItem>> = _stepItems.asStateFlow()

    // ── 上传状态 ─────────────────────────────────────────────

    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    // ── 用户积分 ─────────────────────────────────────────────

    private val _userPoints = MutableStateFlow(userPrefs.points)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    // ── 当前编辑照片的步骤 ID ────────────────────────────────

    private val _pendingPhotoStepId = MutableStateFlow<String?>(null)
    val pendingPhotoStepId: StateFlow<String?> = _pendingPhotoStepId.asStateFlow()

    // ── 方法 ─────────────────────────────────────────────────

    private fun loadLocationsIfEmpty() {
        viewModelScope.launch {
            val cached = cachedLocations.value
            if (cached.isEmpty()) {
                locationRepository.getLocations().collect { /* 缓存到 Room 即可 */ }
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

    fun setPendingPhotoStep(stepId: String) {
        _pendingPhotoStepId.value = stepId
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
                var totalPoints = 0
                for (step in filledSteps) {
                    val imagePart = createImagePart(step)
                    val response = apiService.uploadGuideStep(
                        locationId = targetId.toRequestBody("text/plain".toMediaType()),
                        stepNumber = step.stepNumber.toString()
                            .toRequestBody("text/plain".toMediaType()),
                        description = step.description
                            .toRequestBody("text/plain".toMediaType()),
                        image = imagePart
                    )
                    if (response.success) {
                        totalPoints += response.pointsAwarded
                    }
                }

                userPrefs.addPoints(totalPoints)
                _userPoints.value = userPrefs.points
                _uploadState.value = UploadUiState.Success(
                    "提交成功！获得 $totalPoints 积分，待审核通过后发放"
                )

                // 重置表单
                _selectedLocationId.value = ""
                _selectedLocationName.value = ""
                _stepItems.value = listOf(StepFormItem(stepNumber = 1))
            } catch (e: Exception) {
                _uploadState.value = UploadUiState.Error(
                    "提交失败: ${e.localizedMessage ?: "网络不可用"}"
                )
            }
        }
    }

    fun clearUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    // ── 辅助 ─────────────────────────────────────────────────

    private fun createImagePart(step: StepFormItem): MultipartBody.Part {
        val uri = step.imageUri
        if (uri != null) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: ByteArray(0)
            val body = bytes.toRequestBody(mimeType.toMediaType())
            return MultipartBody.Part.createFormData(
                "image", "step_${step.stepNumber}.jpg", body
            )
        } else {
            val emptyBody = ByteArray(0)
                .toRequestBody("image/jpeg".toMediaType())
            return MultipartBody.Part.createFormData(
                "image", "empty.jpg", emptyBody
            )
        }
    }
}

sealed class UploadUiState {
    data object Idle : UploadUiState()
    data object Loading : UploadUiState()
    data class Success(val message: String) : UploadUiState()
    data class Error(val message: String) : UploadUiState()
}
