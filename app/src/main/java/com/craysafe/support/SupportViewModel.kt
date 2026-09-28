package com.craysafe.support

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.DiagnosticItem
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class SupportViewModel : ViewModel() {

    private val _items = MutableLiveData<List<DiagnosticItem>>(emptyList())
    val items: LiveData<List<DiagnosticItem>> = _items

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Keep the full list for local filtering
    private var allItems: List<DiagnosticItem> = emptyList()

    fun loadQnA(sessionManager: SessionManager) {
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

                val response = ApiClient.apiService.getDiagnosticQnA("Bearer $token")
                _isLoading.value = false

                if (response.success) {
                    allItems = response.data ?: emptyList()
                    _items.value = allItems
                } else {
                    _error.value = response.message ?: "Failed to load Q&A"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    /**
     * Filter the already-loaded list in memory.
     * No network call — instant response.
     */
    fun filterLocally(query: String) {
        if (query.isBlank()) {
            _items.value = allItems
            return
        }
        val q = query.lowercase()
        _items.value = allItems.filter { item ->
            item.issue_title.lowercase().contains(q) ||
                    item.solution.lowercase().contains(q) ||
                    item.category.lowercase().contains(q)
        }
    }
}