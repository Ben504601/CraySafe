package com.craysafe.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.ReportData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    private val _reportData = MutableLiveData<ReportData?>()
    val reportData: LiveData<ReportData?> = _reportData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentRange = "7d"
    private var tankId = -1

    fun loadReport(range: String, sessionManager: SessionManager) {
        currentRange = range
        tankId = sessionManager.getSelectedTankId()
        if (tankId == -1) {
            _error.value = "Select a tank first"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getTankReports("Bearer $token", tankId, range)
                _isLoading.value = false
                if (response.success) {
                    _reportData.value = response.data
                } else {
                    _error.value = response.message ?: "Failed to load reports"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun getCurrentRange() = currentRange
    fun getTankId() = tankId
}