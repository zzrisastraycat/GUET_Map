package com.example.guet_map.module.location.ui.announcement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentAnnouncementDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 公告详情界面
 */
@AndroidEntryPoint
class AnnouncementDetailFragment : Fragment() {

    private var _binding: FragmentAnnouncementDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnnouncementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnouncementDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("announcementId")?.let { id ->
            loadAnnouncement(id)
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadAnnouncement(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.announcements.collect { announcements ->
                    announcements.find { it.id == id }?.let { announcement ->
                        binding.apply {
                            toolbar.title = announcement.title
                            textViewTitle.text = announcement.title
                            textViewContent.text = announcement.content
                            textViewAuthor.text = announcement.author
                            textViewCategory.text = announcement.category.displayName
                            chipPinned.visibility = if (announcement.isPinned) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(announcementId: String) = AnnouncementDetailFragment().apply {
            arguments = Bundle().apply {
                putString("announcementId", announcementId)
            }
        }
    }
}
