package com.example.guet_map

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.guet_map.databinding.ActivityMainBinding
import com.example.guet_map.ui.MainNavViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainNavViewModel: MainNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // #region agent log
        com.example.guet_map.util.AgentDebugLog.log(
            "S3", "MainActivity.onCreate", "start",
            emptyMap(), runId = "crash-fix"
        )
        // #endregion
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainNavViewModel.selectedTab.collectLatest { tabId ->
                    tabId ?: return@collectLatest
                    val consumed = mainNavViewModel.consumeTabRequest()
                    if (consumed != null && navController.currentDestination?.id != consumed) {
                        val menu = binding.bottomNavigation.menu
                        if (menu.findItem(consumed) != null) {
                            binding.bottomNavigation.selectedItemId = consumed
                        } else {
                            try {
                                navController.navigate(consumed)
                            } catch (_: Exception) {
                                // 如果导航失败（目的地可能不存在于 navGraph），忽略以避免二次崩溃
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
}
