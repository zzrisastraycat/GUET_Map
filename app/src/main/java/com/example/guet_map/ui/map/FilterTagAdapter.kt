package com.example.guet_map.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemFilterTagBinding

class FilterTagAdapter(
    private val tags: List<String>,
    private val onTagClick: (String) -> Unit
) : RecyclerView.Adapter<FilterTagAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterTagBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    inner class ViewHolder(
        private val binding: ItemFilterTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: String) {
            binding.tvTagName.text = tag
            binding.root.setOnClickListener { onTagClick(tag) }
        }
    }
}
