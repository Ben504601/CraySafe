package com.craysafe.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.DashboardData
import com.craysafe.api.models.DashboardResponse
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _dashboardData = MutableLiveData<List<DashboardData>>()
    val dashboardData: LiveData<List<DashboardData>> = _dashboardData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadDashboard(sessionManager: SessionManager) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    _error.value = "Please login again"
                    _isLoading.value = false
                    return@launch
                }

                val response = ApiClient.apiService.getDashboard("Bearer $token")
                _isLoading.value = false

                if (response.success) {
                    _dashboardData.value = response.data ?: emptyList()
                } else {
                    _error.value = response.message ?: "Failed to load dashboard"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }
}