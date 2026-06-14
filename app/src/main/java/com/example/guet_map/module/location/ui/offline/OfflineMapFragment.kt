package com.example.guet_map.module.location.ui.offline

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
import com.example.guet_map.databinding.FragmentOfflineMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 离线地图管理界面
 */
@AndroidEntryPoint
class OfflineMapFragment : Fragment() {

    private var _binding: FragmentOfflineMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OfflineMapViewModel by viewModels()

    private lateinit var offlineMapAdapter: OfflineMapAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeState()
    }

    private fun setupRecyclerView() {
        offlineMapAdapter = OfflineMapAdapter(
            onDownloadClick = { map ->
                viewModel.downloadMap(map.id)
            },
            onDeleteClick = { map ->
                viewModel.deleteMap(map.id)
            },
            onPauseClick = { map ->
                viewModel.pauseDownload(map.id)
            }
        )

        binding.recyclerViewMaps.apply {
            adapter = offlineMapAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.offlineMaps.collect { maps ->
                        offlineMapAdapter.submitList(maps)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is OfflineMapUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is OfflineMapUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            }
                            is OfflineMapUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is OfflineMapUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.downloadingMap.collect { downloading ->
                        downloading?.let { (mapId, progress) ->
                            offlineMapAdapter.updateProgress(mapId, progress)
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
        fun newInstance() = OfflineMapFragment()
    }
}
