package com.craysafe.tank

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.TankDetailData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class TankDetailViewModel : ViewModel() {

    private val _data = MutableLiveData<TankDetailData?>()
    val data: LiveData<TankDetailData?> = _data

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadTankDetail(tankId: Int, sessionManager: SessionManager) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Please login again"
                    _isLoading.value = false
                    return@launch
                }

                val response = ApiClient.apiService.getTankDetail(
                    token = "Bearer $token",
                    tankId = tankId
                )

                _isLoading.value = false

                if (response.success && response.data != null) {
                    _data.value = response.data
                } else {
                    _error.value = response.message ?: "Failed to load tank"
                }

            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    // Function to switch mode
    fun switchMode(tankId: Int, newMode: String, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                // TODO: Call API to switch mode
                // After success, reload data:
                loadTankDetail(tankId, sessionManager)
            } catch (e: Exception) {
                _error.value = "Failed to switch mode: $(e.message)"
            }
        }
    }
}