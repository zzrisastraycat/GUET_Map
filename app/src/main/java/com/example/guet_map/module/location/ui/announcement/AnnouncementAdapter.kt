package com.example.guet_map.module.location.ui.announcement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemAnnouncementBinding
import com.example.guet_map.module.location.data.model.Announcement
import java.text.SimpleDateFormat
import java.util.*

/**
 * 公告列表适配器
 */
class AnnouncementAdapter(
    private val onItemClick: (Announcement) -> Unit
) : ListAdapter<Announcement, AnnouncementAdapter.AnnouncementViewHolder>(AnnouncementDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AnnouncementViewHolder(
        private val binding: ItemAnnouncementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(announcement: Announcement) {
            binding.apply {
                textViewTitle.text = announcement.title
                textViewContent.text = announcement.content
                textViewCategory.text = announcement.category.displayName
                textViewTime.text = dateFormat.format(Date(announcement.publishTime))
                textViewViews.text = "${announcement.viewCount} 阅读"

                chipPinned.visibility = if (announcement.isPinned) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                root.setOnClickListener {
                    onItemClick(announcement)
                }
            }
        }
    }

    private class AnnouncementDiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement): Boolean {
            return oldItem == newItem
        }
    }
}
