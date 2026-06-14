package com.example.guet_map.module.social.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.guet_map.databinding.FragmentGalleryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 相册界面
 */
@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels()

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var photoAdapter: PhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeState()
    }

    private fun setupRecyclerViews() {
        albumAdapter = AlbumAdapter { album ->
            viewModel.selectAlbum(album)
            showPhotosView()
        }

        photoAdapter = PhotoAdapter(
            onPhotoClick = { photo ->
                // 查看大图
            },
            onDeleteClick = { photo ->
                viewModel.deletePhoto(photo)
            }
        )

        binding.recyclerViewAlbums.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }

        binding.recyclerViewPhotos.apply {
            adapter = photoAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.albums.collect { albums ->
                        albumAdapter.submitList(albums)
                    }
                }

                launch {
                    viewModel.photos.collect { photos ->
                        photoAdapter.submitList(photos)
                    }
                }

                launch {
                    viewModel.selectedAlbum.collect { album ->
                        updateViewVisibility(album)
                    }
                }

                launch {
                    viewModel.albumsState.collect { state ->
                        when (state) {
                            is GalleryUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is GalleryUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            }
                            is GalleryUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateViewVisibility(album: com.example.guet_map.module.social.data.model.PhotoAlbum?) {
        if (album != null) {
            binding.recyclerViewAlbums.visibility = View.GONE
            binding.recyclerViewPhotos.visibility = View.VISIBLE
            binding.toolbar.title = album.name
            binding.toolbar.setNavigationOnClickListener {
                viewModel.clearSelectedAlbum()
                binding.recyclerViewAlbums.visibility = View.VISIBLE
                binding.recyclerViewPhotos.visibility = View.GONE
                binding.toolbar.title = "相册"
            }
        }
    }

    private fun showPhotosView() {
        binding.recyclerViewAlbums.visibility = View.GONE
        binding.recyclerViewPhotos.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = GalleryFragment()
    }
}
