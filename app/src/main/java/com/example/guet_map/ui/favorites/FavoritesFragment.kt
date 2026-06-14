package com.example.guet_map.ui.favorites

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentFavoritesBinding
import com.example.guet_map.ui.MainNavViewModel
import com.example.guet_map.ui.common.LocationCardAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()

    private lateinit var adapter: LocationCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        adapter = LocationCardAdapter(
            onItemClick = { location ->
                mainNavViewModel.openLocationOnMap(location.locationId)
            },
            onItemLongClick = { location ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("取消收藏")
                    .setMessage("确定取消收藏「${location.name}」？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.removeFavorite(location.locationId)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFavorites.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collectLatest { list ->
                    adapter.submitList(list)
                    binding.textViewEmpty.isVisible = list.isEmpty()
                    binding.recyclerViewFavorites.isVisible = list.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
