package com.example.guet_map.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemSearchResultBinding
import com.example.guet_map.model.Location

class SearchResultAdapter(
    private val onResultClick: (Location) -> Unit
) : ListAdapter<Location, SearchResultAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(location: Location) {
            binding.tvResultName.text = location.name
            binding.tvResultCategory.text = location.category
            binding.root.setOnClickListener { onResultClick(location) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(old: Location, new: Location) =
            old.locationId == new.locationId

        override fun areContentsTheSame(old: Location, new: Location) =
            old == new
    }
}
