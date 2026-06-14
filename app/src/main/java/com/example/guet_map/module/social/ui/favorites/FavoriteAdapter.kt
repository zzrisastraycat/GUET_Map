package com.example.guet_map.module.social.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemFavoriteBinding
import com.example.guet_map.module.social.data.model.Favorite
import java.text.SimpleDateFormat
import java.util.*

/**
 * 收藏列表适配器
 */
class FavoriteAdapter(
    private val onItemClick: (Favorite) -> Unit,
    private val onDeleteClick: (Favorite) -> Unit
) : ListAdapter<Favorite, FavoriteAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: Favorite) {
            binding.apply {
                textViewName.text = favorite.locationName
                textViewCategory.text = favorite.locationCategory
                textViewTime.text = dateFormat.format(Date(favorite.createdAt))

                favorite.note?.let { note ->
                    textViewNote.text = note
                    textViewNote.visibility = android.view.View.VISIBLE
                } ?: run {
                    textViewNote.visibility = android.view.View.GONE
                }

                // 显示标签
                if (favorite.tags.isNotEmpty()) {
                    chipGroupTags.visibility = android.view.View.VISIBLE
                    chipGroupTags.removeAllViews()
                    favorite.tags.take(3).forEach { tag ->
                        val chip = com.google.android.material.chip.Chip(root.context).apply {
                            text = tag
                            isClickable = false
                            textSize = 10f
                        }
                        chipGroupTags.addView(chip)
                    }
                } else {
                    chipGroupTags.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onItemClick(favorite)
                }

                buttonDelete.setOnClickListener {
                    onDeleteClick(favorite)
                }
            }
        }
    }

    private class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
        override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
            return oldItem == newItem
        }
    }
}
