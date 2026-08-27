package com.craysafe.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.LoginResponse
import com.craysafe.api.models.User
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(email, password)
                _isLoading.value = false

                if (response.success) {
                    _loginResult.value = LoginResult.Success(
                        token = response.token ?: "",
                        user = response.user
                    )
                } else {
                    _loginResult.value = LoginResult.Error(response.message)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _loginResult.value = LoginResult.Error("Network error: ${e.message}")
            }
        }
    }
}

sealed class LoginResult {
    data class Success(val token: String, val user: User?) : LoginResult()
    data class Error(val message: String) : LoginResult()
}