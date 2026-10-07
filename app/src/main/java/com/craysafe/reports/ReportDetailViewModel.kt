package com.craysafe.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.ReportData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class ReportDetailViewModel : ViewModel() {
    private val _reportData = MutableLiveData<ReportData?>()
    val reportData: LiveData<ReportData?> = _reportData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load(sessionManager: SessionManager, range: String) {
        val tankId = sessionManager.getSelectedTankId()
        if (tankId == -1) return

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getTankReports("Bearer $token", tankId, range)
                if (response.success) {
                    _reportData.value = response.data
                } else {
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}