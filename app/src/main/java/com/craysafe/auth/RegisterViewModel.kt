package com.craysafe.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craysafe.api.ApiClient
import com.craysafe.api.models.ValidationErrorResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.Gson

class RegisterViewModel : ViewModel() {

    private val _registerResult = MutableLiveData<LoginResult>()
    val registerResult: LiveData<LoginResult> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun register(username: String, email: String, password: String, passwordConfirmation: String, productId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.register(
                    username = username,
                    email = email,
                    password = password,
                    passwordConfirmation = passwordConfirmation,
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
            } catch (e: HttpException) {
                _isLoading.value = false

                if (e.code() == 422) {
                    // Parse validation errors
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        val validationError = Gson().fromJson(
                            errorBody,
                            ValidationErrorResponse::class.java
                        )

                        // Extract the first error message
                        val firstError = validationError.errors?.values?.firstOrNull()?.firstOrNull()
                        _registerResult.value = LoginResult.Error(
                            firstError ?: validationError.message
                        )
                    } catch (parseError: Exception) {
                        _registerResult.value = LoginResult.Error("Validation failed")
                    }
                } else {
                    _registerResult.value = LoginResult.Error("Server error: ${e.code()}")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _registerResult.value = LoginResult.Error("Network error: ${e.message}")
            }
        }
    }
}