package com.craysafe.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.Tank
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class TankPairViewModel : ViewModel() {
    private val _pairResult = MutableLiveData<PairResult>()
    val pairResult: LiveData<PairResult> = _pairResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun pairTank(productId: String, sessionManager: SessionManager) {
        _isLoading.value = true
        val token = sessionManager.getToken()

        if (token == null) {
            _isLoading.value = false
            _pairResult.value = PairResult.Error("Please login again")
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.pairTank("Bearer $token", productId)
                _isLoading.value = false

                if (response.success) {
                    _pairResult.value = PairResult.Success(response.tank)
                } else {
                    _pairResult.value = PairResult.Error(response.message)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _pairResult.value = PairResult.Error("Network error: ${e.message}")
            }
        }
    }
}

sealed class PairResult {
    data class Success(val tank: Tank?) : PairResult()
    data class Error(val message: String) : PairResult()
}