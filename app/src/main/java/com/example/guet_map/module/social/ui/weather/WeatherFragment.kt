package com.example.guet_map.module.social.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.guet_map.databinding.FragmentWeatherBinding
import com.example.guet_map.module.social.data.model.WeatherType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 天气界面
 */
@AndroidEntryPoint
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh()
        observeState()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.weather.collect { weather ->
                        weather?.let { updateWeatherUI(it) }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = state is WeatherUiState.Refreshing

                        when (state) {
                            is WeatherUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.layoutWeather.visibility = View.GONE
                            }
                            is WeatherUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutWeather.visibility = View.VISIBLE
                            }
                            is WeatherUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutWeather.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun updateWeatherUI(weather: com.example.guet_map.module.social.data.model.Weather) {
        binding.apply {
            textViewTemperature.text = "${weather.temperature}°"
            textViewDescription.text = weather.description
            textViewFeelsLike.text = "体感 ${weather.feelsLike}°"
            textViewHumidity.text = "湿度 ${weather.humidity}%"
            textViewWind.text = "${weather.windDirection} ${weather.windSpeed} m/s"

            weather.aqi?.let { aqi ->
                textViewAqi.text = "AQI $aqi"
                textViewAqiLevel.text = weather.aqiLevel
            }

            weather.uvIndex?.let { uv ->
                textViewUvIndex.text = "紫外线 $uv"
            }

            // 根据天气类型设置图标
            val iconRes = when (weather.weatherType) {
                WeatherType.SUNNY -> android.R.drawable.btn_star_big_on
                WeatherType.CLOUDY -> android.R.drawable.btn_star_big_off
                WeatherType.LIGHT_RAIN -> android.R.drawable.presence_away
                else -> android.R.drawable.ic_menu_day
            }
            imageViewWeatherIcon.setImageResource(iconRes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = WeatherFragment()
    }
}
