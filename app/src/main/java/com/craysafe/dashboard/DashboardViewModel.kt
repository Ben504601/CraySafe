package com.craysafe.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.DashboardData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class DashboardViewModel : ViewModel() {

    private val _dashboardData = MutableLiveData<List<DashboardData>>()
    val dashboardData: LiveData<List<DashboardData>> = _dashboardData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var pollingJob: Job? = null
    private var isFetching = false

    fun loadDashboard(sessionManager: SessionManager, isSilent: Boolean = false) {
        if (isFetching) return

        isFetching = true
        if (!isSilent) {
            _isLoading.value = true
        }
        _error.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getDashboard("Bearer $token")

                if (!isSilent) _isLoading.value = false

                if (response.success) {
                    _dashboardData.value = response.data ?: emptyList()
                } else {
                    if (!isSilent) _error.value = response.message ?: "Failed to load"
                }
            } catch (e: Exception) {
                if (!isSilent) {
                    _isLoading.value = false
                    _error.value = "Network error: ${e.message}"
                }
            } finally {
                isFetching = false
                if (!isSilent) _isLoading.value = false
            }
        }
    }

    fun startPolling(sessionManager: SessionManager) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            var delayTime = 5000L
            while (isActive) {
                loadDashboard(sessionManager, isSilent = true)

                if (_error.value != null) {
                    delayTime = (delayTime * 2).coerceAtMost(60000L)
                } else {
                    delayTime = 5000L
                }

                delay(delayTime)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}