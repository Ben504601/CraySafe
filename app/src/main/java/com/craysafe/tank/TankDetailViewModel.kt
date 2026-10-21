package com.craysafe.tank

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.TankDetailData
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class TankDetailViewModel : ViewModel() {

    private val _data = MutableLiveData<TankDetailData?>()
    val data: LiveData<TankDetailData?> = _data

    private val _switchResult = MutableLiveData<String?>()
    val switchResult: LiveData<String?> = _switchResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _renameResult = MutableLiveData<Boolean?>()
    val renameResult: LiveData<Boolean?> = _renameResult

    private var pollingJob: Job? = null

    fun loadTankDetail(tankId: Int, sessionManager: SessionManager, isSilent: Boolean = false) {
        if (!isSilent) {
            _isLoading.value = true
        }

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

    fun startPolling(tankId: Int, sessionManager:SessionManager) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                loadTankDetail(tankId, sessionManager, isSilent = true)
                delay(5000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // Function to switch mode
    fun switchMode(tankId: Int, newMode: String, sessionManager: SessionManager) {
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

                val response = ApiClient.apiService.switchMode(
                    token = "Bearer $token",
                    tankId = tankId,
                    mode = newMode
                )

                if (response.success) {
                    _switchResult.value = response.message

                    loadTankDetail(tankId, sessionManager)
                } else {
                    _isLoading.value = false
                    _error.value = response.message
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to switch mode: ${e.message}"
            }
        }
    }

    fun renameTank(tankId: Int, newName: String, sessionManager: SessionManager) {
        viewModelScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                val response = ApiClient.apiService.renameTank("Bearer $token", tankId, newName)

                if (response.success) {
                    // Update the local data object so the UI updates instantly
                    _data.value = _data.value?.copy(Tankname = newName)
                    _renameResult.value = true
                } else {
                    _renameResult.value = false
                }
            } catch (e: Exception) {
                _renameResult.value = false
            }
        }
    }

    fun resetRenameState() {
        _renameResult.value = null
    }
}