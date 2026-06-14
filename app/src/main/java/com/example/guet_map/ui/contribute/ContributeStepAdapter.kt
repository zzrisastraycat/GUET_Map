package com.example.guet_map.ui.contribute

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemStepFormBinding

data class StepFormItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val stepNumber: Int,
    val description: String = "",
    val imageUri: Uri? = null
)

class ContributeStepAdapter(
    private val onPhotoClick: (String) -> Unit,
    private val onDescriptionChanged: (String, String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<StepFormItem, ContributeStepAdapter.ViewHolder>(StepFormDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStepFormBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemStepFormBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StepFormItem) {
            val stepNum = item.stepNumber.toString().padStart(2, '0')
            binding.tvStepLabel.text = "步骤 $stepNum"

            // Photo area
            if (item.imageUri != null) {
                binding.ivStepPhoto.load(item.imageUri) {
                    crossfade(200)
                    transformations(RoundedCornersTransformation(8f))
                    placeholder(R.drawable.bg_image_placeholder)
                }
                binding.ivCameraIcon.visibility = android.view.View.GONE
            } else {
                binding.ivStepPhoto.setImageResource(R.drawable.bg_image_placeholder)
                binding.ivCameraIcon.visibility = android.view.View.VISIBLE
            }

            // Description - only update if different to avoid cursor issues
            val currentText = binding.etDescription.text.toString()
            if (currentText != item.description) {
                binding.etDescription.setText(item.description)
            }

            // Listeners — remove old ones to avoid duplicate triggers
            binding.etDescription.setOnFocusChangeListener(null)
            binding.etDescription.setOnFocusChangeListener { _, _ ->
                onDescriptionChanged(item.id, binding.etDescription.text.toString())
            }

            binding.ivStepPhoto.setOnClickListener {
                onPhotoClick(item.id)
            }
            binding.ivCameraIcon.setOnClickListener {
                onPhotoClick(item.id)
            }

            binding.btnRemoveStep.setOnClickListener {
                onDeleteClick(item.id)
            }
        }
    }
}

class StepFormDiffCallback : DiffUtil.ItemCallback<StepFormItem>() {
    override fun areItemsTheSame(oldItem: StepFormItem, newItem: StepFormItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: StepFormItem, newItem: StepFormItem): Boolean {
        return oldItem == newItem
    }
}
