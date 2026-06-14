package com.example.guet_map.module.location.ui.announcement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentAnnouncementBinding
import com.example.guet_map.R
import com.example.guet_map.module.location.data.model.AnnouncementCategory
import com.google.android.material.chip.ChipGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 公告列表界面
 */
@AndroidEntryPoint
class AnnouncementFragment : Fragment() {

    private var _binding: FragmentAnnouncementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnouncementViewModel by viewModels()

    private lateinit var announcementAdapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): SwipeRefreshLayout {
        _binding = FragmentAnnouncementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupCategoryChips()
        observeState()
    }

    private fun setupRecyclerView() {
        announcementAdapter = AnnouncementAdapter { announcement ->
            // TODO: 导航到公告详情页面
            // 需要在 Activity 中处理导航，使用 Navigation Component 或手动 Fragment 替换
        }

        binding.recyclerViewAnnouncements.apply {
            adapter = announcementAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when {
                checkedIds.contains(binding.chipAcademic.id) -> AnnouncementCategory.ACADEMIC
                checkedIds.contains(binding.chipActivity.id) -> AnnouncementCategory.ACTIVITY
                checkedIds.contains(binding.chipSystem.id) -> AnnouncementCategory.SYSTEM
                checkedIds.contains(binding.chipEmergency.id) -> AnnouncementCategory.EMERGENCY
                else -> null
            }
            viewModel.selectCategory(category)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.announcements.collect { announcements ->
                        announcementAdapter.submitList(announcements)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = false

                        when (state) {
                            is AnnouncementUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is AnnouncementUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            }
                            is AnnouncementUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is AnnouncementUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AnnouncementFragment()
    }
}
