package com.example.guet_map.module.social.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentFavoritesBinding
import com.example.guet_map.module.social.data.model.FavoriteCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 收藏列表界面
 */
@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()

    private lateinit var favoriteAdapter: FavoriteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        observeState()
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter(
            onItemClick = { favorite ->
                // 跳转到地图定位
            },
            onDeleteClick = { favorite ->
                viewModel.removeFavorite(favorite)
            }
        )

        binding.recyclerViewFavorites.apply {
            adapter = favoriteAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when {
                checkedIds.contains(binding.chipFood.id) -> FavoriteCategory.FOOD
                checkedIds.contains(binding.chipStudy.id) -> FavoriteCategory.STUDY
                checkedIds.contains(binding.chipSports.id) -> FavoriteCategory.SPORTS
                checkedIds.contains(binding.chipEntertainment.id) -> FavoriteCategory.ENTERTAINMENT
                checkedIds.contains(binding.chipDaily.id) -> FavoriteCategory.DAILY
                else -> FavoriteCategory.ALL
            }
            viewModel.selectCategory(category)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favorites.collect { favorites ->
                        favoriteAdapter.submitList(favorites)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is FavoritesUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is FavoritesUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            }
                            is FavoritesUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                            }
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
        fun newInstance() = FavoritesFragment()
    }
}
