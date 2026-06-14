package com.example.guet_map.module.social.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemAlbumBinding
import com.example.guet_map.module.social.data.model.PhotoAlbum

/**
 * 相册列表适配器
 */
class AlbumAdapter(
    private val onAlbumClick: (PhotoAlbum) -> Unit
) : ListAdapter<PhotoAlbum, AlbumAdapter.AlbumViewHolder>(AlbumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlbumViewHolder(
        private val binding: ItemAlbumBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: PhotoAlbum) {
            binding.apply {
                textViewName.text = album.name
                textViewCount.text = "${album.photoCount} 张照片"

                album.coverUrl?.let { url ->
                    imageViewCover.load(url) {
                        placeholder(R.drawable.ic_photo)
                        error(R.drawable.ic_photo)
                    }
                } ?: run {
                    imageViewCover.setImageResource(R.drawable.ic_photo)
                }

                root.setOnClickListener {
                    onAlbumClick(album)
                }
            }
        }
    }

    private class AlbumDiffCallback : DiffUtil.ItemCallback<PhotoAlbum>() {
        override fun areItemsTheSame(oldItem: PhotoAlbum, newItem: PhotoAlbum): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoAlbum, newItem: PhotoAlbum): Boolean {
            return oldItem == newItem
        }
    }
}
