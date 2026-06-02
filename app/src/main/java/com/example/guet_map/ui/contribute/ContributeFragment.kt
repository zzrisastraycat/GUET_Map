package com.example.guet_map.ui.contribute

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.flow.collectLatest
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
    ) { uri ->
        uri?.let { u ->
            viewModel.pendingPhotoStepId.value?.let { stepId ->
                viewModel.setImageUri(stepId, u)
                viewModel.setPendingPhotoStep("")
            }
        }
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
                photoPickerLauncher.launch("image/*")
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
            viewModel.selectLocation(
                locationId = "",   // 自定义输入时无 ID
                locationName = selected
            )
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
                        stepAdapter.submitList(steps)
                        binding.tvStepCount.text = "共 ${steps.size} 步"
                    }
                }

                // 用户积分
                launch {
                    viewModel.userPoints.collectLatest { points ->
                        binding.tvPoints.text = points.toString()
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
