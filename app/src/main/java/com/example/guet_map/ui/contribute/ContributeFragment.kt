package com.example.guet_map.ui.contribute

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentContributeBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.guet_map.model.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContributeFragment : Fragment(R.layout.fragment_contribute) {

    private var _binding: FragmentContributeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContributeViewModel by viewModels()
    private lateinit var stepAdapter: ContributeStepAdapter
    private var locationNames: List<String> = emptyList()

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> applyPhotoUri(uri) }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "H3",
            "ContributeFragment.cameraLauncher",
            "result",
            mapOf("success" to success, "hasUri" to (pendingCameraUri != null))
        )
        // #endregion
        if (success) pendingCameraUri?.let { applyPhotoUri(it) }
        pendingCameraUri = null
    }

    private var pendingCameraUri: android.net.Uri? = null

    private fun applyPhotoUri(uri: android.net.Uri?) {
        if (uri == null) return
        val stepId = viewModel.pendingPhotoStepId.value
        if (stepId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "未关联到步骤，请重新点击照片区域", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.setImageUri(stepId, uri)
        viewModel.setPendingPhotoStep(null)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "C1", "ContributeFragment.cameraPermission", "result",
            mapOf("granted" to granted), runId = "post-fix2"
        )
        // #endregion
        if (granted) launchCameraInternal()
        else Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContributeBinding.bind(view)

        setupStepRecycler()
        setupLocationDropdown()
        setupButtons()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── RecyclerView ──────────────────────────────────────────

    private fun setupStepRecycler() {
        stepAdapter = ContributeStepAdapter(
            onPhotoClick = { stepId ->
                viewModel.setPendingPhotoStep(stepId)
                showPhotoSourceDialog()
            },
            onDescriptionChanged = { stepId, text ->
                viewModel.updateDescription(stepId, text)
            },
            onDeleteClick = { stepId ->
                viewModel.removeStep(stepId)
            }
        )
        binding.rvStepForms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStepForms.adapter = stepAdapter
    }

    // ── Location dropdown ─────────────────────────────────────

    private fun setupLocationDropdown() {
        binding.actvLocation.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            val loc = viewModel.cachedLocations.value.find { it.name == selected }
            if (loc != null) {
                viewModel.selectLocation(loc.locationId, loc.name)
            } else {
                viewModel.selectLocation("", selected)
            }
        }
    }

    private fun showPhotoSourceDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加照片")
            .setItems(arrayOf("相册", "拍照")) { _, which ->
                when (which) {
                    0 -> photoPickerLauncher.launch("image/*")
                    1 -> launchCamera()
                }
            }
            .show()
    }

    private fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCameraInternal()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraInternal() {
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "C1", "ContributeFragment.launchCameraInternal", "start", emptyMap(), runId = "post-fix2"
        )
        // #endregion
        try {
            val photoFile = java.io.File.createTempFile(
                "capture_", ".jpg", requireContext().cacheDir
            )
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            com.example.guet_map.util.AgentDebugLog.log(
                "C1",
                "ContributeFragment.launchCameraInternal",
                "error",
                mapOf("error" to (e.message ?: e.javaClass.simpleName)),
                runId = "post-fix2"
            )
            Toast.makeText(requireContext(), "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLocationSuggestions(locations: List<com.example.guet_map.model.Location>) {
        locationNames = locations.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            locationNames
        )
        binding.actvLocation.setAdapter(adapter)
    }

    // ── Buttons ───────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnAddStep.setOnClickListener {
            viewModel.addStep()
            binding.rvStepForms.smoothScrollToPosition(stepAdapter.itemCount - 1)
        }

        binding.btnSubmit.setOnClickListener {
            viewModel.submitSteps()
        }

        binding.btnSaveDraft.setOnClickListener { viewModel.saveDraft() }
        binding.btnLoadDraft.setOnClickListener { viewModel.loadDraft() }
        binding.btnMySubmissions.setOnClickListener { showMySubmissionsDialog() }
    }

    private fun showMySubmissionsDialog() {
        viewModel.loadMySubmissions()
        viewLifecycleOwner.lifecycleScope.launch {
            val resource = viewModel.mySubmissions
                .filter { it !is Resource.Loading }
                .first()
            when (resource) {
                is Resource.Success -> {
                    val lines = resource.data.joinToString("\n\n") { item ->
                        "${item.locationName} · 步骤${item.stepNumber}\n" +
                            "状态：${statusLabel(item.status)}\n" +
                            item.description +
                            (item.rejectReason?.let { "\n驳回：$it" } ?: "")
                    }.ifBlank { "暂无提交记录" }
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.my_submissions))
                        .setMessage(lines)
                        .setPositiveButton("确定", null)
                        .show()
                }
                is Resource.Error ->
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    private fun statusLabel(status: String) = when (status) {
        "approved" -> "已通过"
        "rejected" -> "已驳回"
        "pending" -> "待审核"
        else -> status
    }

    // ── ViewModel observers ───────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 地点列表 → 更新下拉建议
                launch {
                    viewModel.cachedLocations.collectLatest { locations ->
                        if (locations.isNotEmpty()) {
                            updateLocationSuggestions(locations)
                        }
                    }
                }

                // 步骤表单更新
                launch {
                    viewModel.stepItems.collectLatest { steps ->
                        // 延迟到主线程空闲时更新，避免 RecyclerView 布局冲突
                        binding.rvStepForms.post {
                            if (_binding != null) {
                                stepAdapter.submitList(steps)
                                binding.tvStepCount.text = "共 ${steps.size} 步"
                            }
                        }
                    }
                }

                // 用户积分
                launch {
                    viewModel.userPoints.collectLatest { points ->
                        binding.tvPoints.text = points.toString()
                    }
                }

                launch {
                    viewModel.selectedLocationName.collectLatest { name ->
                        if (name.isNotBlank()) {
                            binding.actvLocation.setText(name, false)
                        }
                    }
                }

                // 上传状态
                launch {
                    viewModel.uploadState.collectLatest { state ->
                        when (state) {
                            is UploadUiState.Idle -> hideUploadStatus()
                            is UploadUiState.Loading -> showUploadLoading()
                            is UploadUiState.Success -> showUploadResult(state.message)
                            is UploadUiState.Error -> showUploadResult(state.message)
                        }
                    }
                }
            }
        }
    }

    // ── Upload UI states ──────────────────────────────────────

    private fun hideUploadStatus() {
        binding.llUploadStatus.visibility = View.GONE
        binding.btnSubmit.isEnabled = true
    }

    private fun showUploadLoading() {
        binding.llUploadStatus.visibility = View.VISIBLE
        binding.progressUpload.visibility = View.VISIBLE
        binding.tvUploadMessage.visibility = View.GONE
        binding.btnSubmit.isEnabled = false
    }

    private fun showUploadResult(message: String) {
        binding.llUploadStatus.visibility = View.VISIBLE
        binding.progressUpload.visibility = View.GONE
        binding.tvUploadMessage.visibility = View.VISIBLE
        binding.tvUploadMessage.text = message
        binding.btnSubmit.isEnabled = true

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // 成功时 2 秒后自动隐藏
        if (message.startsWith("提交成功")) {
            view?.postDelayed({
                viewModel.clearUploadState()
            }, 3000)
        }
    }
}
