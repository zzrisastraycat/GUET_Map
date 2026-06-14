package com.example.guet_map.module.social.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.module.social.data.model.Weather
import com.example.guet_map.module.social.domain.usecase.GetWeatherUseCase
import com.example.guet_map.module.social.domain.usecase.RefreshWeatherUseCase
import com.example.guet_map.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 天气 ViewModel
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val refreshWeatherUseCase: RefreshWeatherUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    init {
        loadWeather()
    }

    private fun loadWeather() {
        viewModelScope.launch {
            when (val result = getWeatherUseCase()) {
                is Resource.Success -> {
                    _weather.value = result.data
                    _uiState.value = WeatherUiState.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = WeatherUiState.Error(result.message)
                }
                is Resource.Loading -> {
                    _uiState.value = WeatherUiState.Loading
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Refreshing

            when (val result = refreshWeatherUseCase()) {
                is Resource.Success -> {
                    _weather.value = result.data
                    _uiState.value = WeatherUiState.Success(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = WeatherUiState.Error(result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }
}

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data object Refreshing : WeatherUiState()
    data class Success(val weather: Weather) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
