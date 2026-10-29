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

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentFilter = "all"
    private var currentPage = 1
    private var hasMorePages = true

    // Keep a master list to append to during pagination
    private val allAlerts = mutableListOf<Alert>()

    fun loadAlerts(sessionManager: SessionManager, filter: String = currentFilter) {
        currentFilter = filter
        currentPage = 1
        hasMorePages = true
        allAlerts.clear()

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

                val response = ApiClient.apiService.getAlerts(
                    token = "Bearer $token",
                    page = currentPage,
                    filter = currentFilter
                )

                _isLoading.value = false

                if (response.success && response.data != null) {
                    val fetchedAlerts = response.data.alerts ?: emptyList()
                    allAlerts.addAll(fetchedAlerts)
                    _alerts.value = allAlerts.toList()
                    _unreadCount.value = response.data.unread_count

                    hasMorePages = response.data.pagination?.has_more ?: false
                } else {
                    _error.value = response.message ?: "Failed to load alerts"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun loadNextPage(sessionManager: SessionManager) {
        if (!hasMorePages || _isLoadingMore.value == true || _isLoading.value == true) return

        currentPage++
        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getAlerts(
                    token = "Bearer $token",
                    page = currentPage,
                    filter = currentFilter
                )

                _isLoadingMore.value = false

                if (response.success && response.data != null) {
                    val fetchedAlerts = response.data.alerts ?: emptyList()
                    allAlerts.addAll(fetchedAlerts)
                    _alerts.value = allAlerts.toList()
                    _unreadCount.value = response.data.unread_count

                    hasMorePages = response.data.pagination?.has_more ?: false
                } else {
                    // Failed to load page, roll back page count so retry works
                    currentPage--
                }
            } catch (e: Exception) {
                _isLoadingMore.value = false
                currentPage-- // Rollback
            }
        }
    }

    fun markAsRead(alertId: Int, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.markAlertRead("Bearer $token", alertId)

                if (response.success) {
                    // Update the local list
                    val index = allAlerts.indexOfFirst { it.alert_id == alertId }
                    if (index != -1) {
                        allAlerts[index] = allAlerts[index].copy(status = "read")
                        _alerts.value = allAlerts.toList()
                        _unreadCount.value = maxOf(0, (_unreadCount.value ?: 1) - 1)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to mark as read: ${e.message}"
            }
        }
    }

    fun markAllAsRead(sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.markAllAlertsRead("Bearer $token")

                if (response.success) {
                    // 1. Update the master list in memory to "read"
                    allAlerts.replaceAll { it.copy(status = "read") }

                    // 2. Update the UI based on the active filter
                    if (currentFilter == "unread") {
                        // If we are on the "Unread" tab, the list should now be empty
                        allAlerts.clear()
                        _alerts.value = emptyList()
                    } else {
                        // If we are on the "All" tab, just refresh the list to show them as read
                        _alerts.value = allAlerts.toList()
                    }

                    // 3. Clear the badge count
                    _unreadCount.value = 0
                } else {
                    _error.value = response.message ?: "Failed to mark all as read"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }
}