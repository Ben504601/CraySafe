package com.craysafe.alerts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.Alert
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {

    private val _alerts = MutableLiveData<List<Alert>>(emptyList())
    val alerts: LiveData<List<Alert>> = _alerts

    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadAlerts(sessionManager: SessionManager) {
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

                val response = ApiClient.apiService.getAlerts("Bearer $token")
                _isLoading.value = false

                if (response.success) {
                    _alerts.value = response.data ?: emptyList()
                    _unreadCount.value = response.unread_count ?: 0
                } else {
                    _error.value = response.message ?: "Failed to load alerts"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun markAsRead(alertId: Int, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.markAlertRead("Bearer $token", alertId)

                if (response.success) {
                    val updated = _alerts.value?.map { alert ->
                        if (alert.alert_id == alertId) alert.copy(status = "read") else alert
                    } ?: emptyList()
                    _alerts.value = updated
                    _unreadCount.value = updated.count { it.status == "unread" }
                }
            } catch (e: Exception) {
                _error.value = "Failed to mark as read: ${e.message}"
            }
        }
    }
}