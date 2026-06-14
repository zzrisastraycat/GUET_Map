package com.example.guet_map.ui.map.component

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemFilterTagBinding

class FilterTagAdapter(
    private val tags: List<String>,
    private val onTagClick: (String?) -> Unit
) : RecyclerView.Adapter<FilterTagAdapter.ViewHolder>() {

    private var selectedTag: String? = null

    fun setSelectedTag(tag: String?) {
        val old = selectedTag
        selectedTag = tag
        tags.forEachIndexed { index, t ->
            if (t == old || t == tag) notifyItemChanged(index)
        }
    }

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
            val selected = tag == selectedTag
            val ctx = binding.root.context
            if (selected) {
                binding.root.setBackgroundResource(R.drawable.bg_category_chip)
                binding.tvTagName.setTextColor(
                    ContextCompat.getColor(ctx, R.color.primary)
                )
            } else {
                binding.root.setBackgroundResource(R.drawable.bg_search_bar)
                binding.tvTagName.setTextColor(
                    ContextCompat.getColor(ctx, android.R.color.black)
                )
            }
            binding.root.setOnClickListener {
                val newSelection = if (selectedTag == tag) null else tag
                setSelectedTag(newSelection)
                onTagClick(newSelection)
            }
        }
    }
}
