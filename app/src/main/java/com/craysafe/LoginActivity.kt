package com.craysafe

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.craysafe.auth.LoginResult
import com.craysafe.auth.LoginViewModel
import com.craysafe.databinding.ActivityLoginBinding
import com.craysafe.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Already logged in? Skip
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility =
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.tilEmail.isEnabled = !isLoading
            binding.tilPassword.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> onLoginSuccess(result)
                is LoginResult.Error -> {
                    Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onLoginSuccess(result: LoginResult.Success) {
        result.user?.let { user ->
            sessionManager.saveAuth(
                token = result.token,
                userId = user.id,
                username = user.username,
                email = user.email,
                role = user.role
            )
        }

        Toast.makeText(this, "Welcome back, ${result.user?.username ?: ""}!", Toast.LENGTH_SHORT).show()

        // Register FCM token silently
        registerFcmToken()

        // Navigate to main
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            hideKeyboard()

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun registerFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fcmToken = task.result
                    lifecycleScope.launch {
                        try {
                            com.craysafe.api.ApiClient.apiService.saveFcmToken(
                                "Bearer ${sessionManager.getToken()}",
                                fcmToken
                            )
                        } catch (_: Exception) {
                            // Silent — will retry on next launch
                        }
                    }
                }
            }
    }
}