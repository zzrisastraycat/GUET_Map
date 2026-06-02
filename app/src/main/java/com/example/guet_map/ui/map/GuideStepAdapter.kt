package com.example.guet_map.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemGuideStepBinding
import com.example.guet_map.model.GuideStep

class GuideStepAdapter : ListAdapter<GuideStep, GuideStepAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGuideStepBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    inner class ViewHolder(
        private val binding: ItemGuideStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: GuideStep, isLast: Boolean) {
            val stepNum = step.stepNumber.toString().padStart(2, '0')
            binding.tvStepNumber.text = stepNum
            binding.tvDescription.text = step.description

            // 最后一步隐藏竖线
            binding.vLine.visibility = if (isLast) android.view.View.INVISIBLE
                else android.view.View.VISIBLE

            // Coil 加载实景照片
            if (step.imageUrl.isNotBlank()) {
                binding.ivStepPhoto.visibility = android.view.View.VISIBLE
                binding.ivStepPhoto.load(step.imageUrl) {
                    crossfade(300)
                    transformations(RoundedCornersTransformation(8f))
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            } else {
                binding.ivStepPhoto.visibility = android.view.View.GONE
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<GuideStep>() {
        override fun areItemsTheSame(old: GuideStep, new: GuideStep) =
            old.id == new.id

        override fun areContentsTheSame(old: GuideStep, new: GuideStep) =
            old == new
    }
}
