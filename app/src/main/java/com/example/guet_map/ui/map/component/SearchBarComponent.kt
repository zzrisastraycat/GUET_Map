package com.example.guet_map.ui.map.component

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentMapBinding
import com.example.guet_map.model.Location
import com.example.guet_map.ui.map.SearchResultAdapter

/**
 * 搜索栏组件
 * 负责：搜索输入、搜索结果展示
 * 注意：语音搜索功能由 Fragment 直接处理
 */
class SearchBarComponent(
    private val context: Context,
    private val binding: FragmentMapBinding,
    private val onQueryChanged: (String) -> Unit,
    private val onSearchSubmit: (String) -> Unit,
    private val onLocationPicked: (Location) -> Unit
) {
    private lateinit var searchAdapter: SearchResultAdapter
    private var suppressSearchResultsUntilEdit = false

    /**
     * 设置搜索适配器和输入监听
     */
    fun setup() {
        setupSearchInput()
        setupSearchResults()
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                suppressSearchResultsUntilEdit = false
                onQueryChanged(query)
                if (query.isBlank()) {
                    binding.cardSearchResults.isVisible = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val q = binding.etSearch.text?.toString().orEmpty()
                if (q.isNotBlank()) {
                    onSearchSubmit(q)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupSearchResults() {
        searchAdapter = SearchResultAdapter { location ->
            binding.etSearch.setText(location.name)
            binding.etSearch.setSelection(location.name.length)
            onLocationPicked(location)
        }
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    /**
     * 更新搜索结果
     */
    fun updateSearchResults(results: List<Location>) {
        if (suppressSearchResultsUntilEdit) {
            binding.cardSearchResults.isVisible = false
            return
        }
        if (results.isNotEmpty()) {
            binding.cardSearchResults.isVisible = true
            searchAdapter.submitList(results)
        } else {
            binding.cardSearchResults.isVisible = false
        }
    }

    /**
     * 清除搜索结果
     */
    fun dismissSearchResults() {
        binding.cardSearchResults.isVisible = false
        suppressSearchResultsUntilEdit = true
        hideKeyboard()
    }

    /**
     * 清除搜索输入
     */
    fun clearSearchInput() {
        binding.etSearch.setText("")
        binding.etSearch.clearFocus()
    }

    /**
     * 获取当前搜索关键词
     */
    fun getCurrentQuery(): String = binding.etSearch.text?.toString().orEmpty()

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }
}
