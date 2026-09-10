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

    private val _tankData = MutableLiveData<TankDetailData>()
    val tankData: LiveData<TankDetailData> = _tankData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadTankDetail(tankId: Int, sessionManager: SessionManager) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Please login again"
                    _isLoading.value = false
                    return@launch
                }

                // TODO: Call API to get tank Details
                // val response = ApiClient.apiService.getTankDetail("Bearer $token", tankId)

                // test data
                val mockData = TankDetailData(
                    tankName = "Tank $tankId",
                    mode = "Growing",
                    temperature = 25.5,
                    phLevel = 7.4,
                    turbidity = 15.0,
                    status = "Safe",
                    lastUpdated = "2:30 PM",
                    timeToDanger = "8 hours",
                    tankId = tankId
                )

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
                val token = sessionManager.getToken()
                if (token == null) return@launch

                // TODO: Call API to switch mode
                // val response = ApiClient.apiService.switchMode("Bearer $token", tankId, newMode)

                // local update
                val currentData = _tankData.value
                if (currentData != null) {
                    _tankData.value = currentData.copy(mode = newMode)
                }
            } catch (e: Exception) {
                _error.value = "Failed to switch mode: ${e.message}"
            }
        }
    }
}