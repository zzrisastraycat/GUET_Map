package com.example.guet_map.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        viewModel.refresh()

        binding.btnLoginAction.setOnClickListener {
            if (viewModel.uiState.value.isLoggedIn) {
                viewModel.logout()
            } else {
                val username = binding.etUsername.text?.toString().orEmpty()
                val password = binding.etPassword.text?.toString().orEmpty()
                if (username.isBlank()) {
                    binding.tilUsername.error = getString(R.string.login_username_required)
                    return@setOnClickListener
                }
                binding.tilUsername.error = null
                viewModel.login(username, password)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.btnLoginAction.isEnabled = !state.loading
        binding.btnLoginAction.text = if (state.isLoggedIn) {
            getString(R.string.logout_action)
        } else {
            getString(R.string.login_action)
        }

        if (state.isLoggedIn) {
            binding.tilUsername.visibility = View.GONE
            binding.tilPassword.visibility = View.GONE
            binding.tvLoggedInInfo.visibility = View.VISIBLE
            binding.tvLoggedInInfo.text = getString(
                R.string.login_logged_in_format,
                state.nickname,
                state.userId,
                state.points
            )
        } else {
            binding.tilUsername.visibility = View.VISIBLE
            binding.tilPassword.visibility = View.VISIBLE
            binding.tvLoggedInInfo.visibility = View.GONE
        }

        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
