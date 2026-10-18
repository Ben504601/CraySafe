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

    // Keep the full list in memory to allow instant filtering
    private var allAlerts: List<Alert> = emptyList()
    private var showUnreadOnly = false

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
                    allAlerts = response.data?.alerts ?: emptyList()
                    applyFilter()
                } else {
                    _error.value = response.message ?: "Failed to load alerts"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun setFilter(unreadOnly: Boolean) {
        if (showUnreadOnly == unreadOnly) return
        showUnreadOnly = unreadOnly
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (showUnreadOnly) {
            allAlerts.filter { it.status == "unread" }
        } else {
            allAlerts
        }

        _alerts.value = filtered
        _unreadCount.value = allAlerts.count { it.status == "unread" }
    }

    fun markAsRead(alertId: Int, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.markAlertRead("Bearer $token", alertId)

                if (response.success) {
                    // Update the master list
                    allAlerts = allAlerts.map { alert ->
                        if (alert.alert_id == alertId) alert.copy(status = "read") else alert
                    }
                    // Re-apply the current filter to update the UI
                    applyFilter()
                }
            } catch (e: Exception) {
                _error.value = "Failed to mark as read: ${e.message}"
            }
        }
    }
}