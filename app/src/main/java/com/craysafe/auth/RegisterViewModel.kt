package com.craysafe.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.User
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _registerResult = MutableLiveData<LoginResult>()
    val registerResult: LiveData<LoginResult> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(username: String, email: String, password: String, confirm_password: String, productId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.register(
                    username = username,
                    email = email,
                    password = password,
                    confirm_password = confirm_password,
                    productId = productId
                )
                _isLoading.value = false

                if (response.success) {
                    _registerResult.value = LoginResult.Success(
                        token = response.token ?: "",
                        user = response.user
                    )
                } else {
                    _registerResult.value = LoginResult.Error(response.message)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _registerResult.value = LoginResult.Error("Network error: ${e.message}")
            }
        }
    }
}