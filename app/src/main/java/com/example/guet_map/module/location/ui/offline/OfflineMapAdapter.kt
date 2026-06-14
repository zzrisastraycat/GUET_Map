package com.example.guet_map.module.location.ui.offline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemOfflineMapBinding
import com.example.guet_map.module.location.data.model.DownloadStatus
import com.example.guet_map.module.location.data.model.OfflineMap

/**
 * 离线地图列表适配器
 */
class OfflineMapAdapter(
    private val onDownloadClick: (OfflineMap) -> Unit,
    private val onDeleteClick: (OfflineMap) -> Unit,
    private val onPauseClick: (OfflineMap) -> Unit
) : ListAdapter<OfflineMap, OfflineMapAdapter.OfflineMapViewHolder>(OfflineMapDiffCallback()) {

    private val progressMap = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineMapViewHolder {
        val binding = ItemOfflineMapBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OfflineMapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfflineMapViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateProgress(mapId: String, progress: Int) {
        progressMap[mapId] = progress
        val position = currentList.indexOfFirst { it.id == mapId }
        if (position != -1) {
            notifyItemChanged(position, "progress")
        }
    }

    inner class OfflineMapViewHolder(
        private val binding: ItemOfflineMapBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(map: OfflineMap) {
            val currentProgress = progressMap[map.id] ?: map.progress

            binding.apply {
                textViewName.text = map.name
                textViewDescription.text = map.description
                textViewSize.text = formatSize(map.size)
                textViewVersion.text = "v${map.version}"

                // 根据状态显示不同 UI
                when (map.status) {
                    DownloadStatus.NOT_DOWNLOADED -> {
                        progressBar.visibility = View.GONE
                        buttonAction.text = "下载"
                        buttonAction.setOnClickListener { onDownloadClick(map) }
                        buttonDelete.visibility = View.GONE
                    }
                    DownloadStatus.DOWNLOADING -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = currentProgress
                        textViewProgress.visibility = View.VISIBLE
                        textViewProgress.text = "${currentProgress}%"
                        buttonAction.text = "暂停"
                        buttonAction.setOnClickListener { onPauseClick(map) }
                        buttonDelete.visibility = View.GONE
                    }
                    DownloadStatus.PAUSED -> {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = currentProgress
                        textViewProgress.visibility = View.VISIBLE
                        textViewProgress.text = "已暂停"
                        buttonAction.text = "继续"
                        buttonAction.setOnClickListener { onDownloadClick(map) }
                        buttonDelete.visibility = View.VISIBLE
                    }
                    DownloadStatus.DOWNLOADED -> {
                        progressBar.visibility = View.GONE
                        textViewProgress.visibility = View.GONE
                        buttonAction.visibility = View.GONE
                        buttonDelete.visibility = View.VISIBLE
                        buttonDelete.setOnClickListener { onDeleteClick(map) }
                    }
                    DownloadStatus.UPDATE_AVAILABLE -> {
                        progressBar.visibility = View.GONE
                        textViewProgress.visibility = View.VISIBLE
                        textViewProgress.text = "有更新"
                        buttonAction.text = "更新"
                        buttonAction.setOnClickListener { onDownloadClick(map) }
                        buttonDelete.visibility = View.GONE
                    }
                    DownloadStatus.FAILED -> {
                        progressBar.visibility = View.GONE
                        textViewProgress.visibility = View.VISIBLE
                        textViewProgress.text = "下载失败"
                        buttonAction.text = "重试"
                        buttonAction.setOnClickListener { onDownloadClick(map) }
                        buttonDelete.visibility = View.GONE
                    }
                }
            }
        }

        private fun formatSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }

    private class OfflineMapDiffCallback : DiffUtil.ItemCallback<OfflineMap>() {
        override fun areItemsTheSame(oldItem: OfflineMap, newItem: OfflineMap): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OfflineMap, newItem: OfflineMap): Boolean {
            return oldItem == newItem
        }
    }
}
