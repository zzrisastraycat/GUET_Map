package com.example.guet_map.module.social.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemPhotoBinding
import com.example.guet_map.module.social.data.model.Photo

/**
 * 照片网格适配器
 */
class PhotoAdapter(
    private val onPhotoClick: (Photo) -> Unit,
    private val onDeleteClick: (Photo) -> Unit
) : ListAdapter<Photo, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            binding.apply {
                val imageUrl = photo.thumbnailUrl ?: photo.url
                imageViewPhoto.load(imageUrl) {
                    placeholder(R.drawable.ic_photo)
                    error(R.drawable.ic_photo)
                }

                photo.locationName?.let { location ->
                    textViewLocation.text = location
                    textViewLocation.visibility = android.view.View.VISIBLE
                } ?: run {
                    textViewLocation.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onPhotoClick(photo)
                }

                root.setOnLongClickListener {
                    onDeleteClick(photo)
                    true
                }
            }
        }
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean {
            return oldItem == newItem
        }
    }
}
